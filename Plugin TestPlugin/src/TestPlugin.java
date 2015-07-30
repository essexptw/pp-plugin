import java.util.ArrayList;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.niklabs.perfectplayer.plugin.PageDownloader;
import com.niklabs.perfectplayer.plugin.Plugin;

/**
 * Plugin 'TestPlugin' for the Perfect Player
 * @version 0.1.0
 */
public class TestPlugin implements Plugin {
	private String currURL = "http://niklabs.com/photos-page-1";
	
	private ArrayList<String> urlsHistory = new ArrayList<String>();
	
	private String pageText = "";
	
	private String[] names = null;
	private String[] urls = null;
	private int[] types = null;
	private String[] descriptions = null;
	private String[] thumbsURLs = null;
	
	private Object icon = null;
	
	private String nextPageURL = null;
	
	public TestPlugin() {}
	
	@Override
	public String getPluginName() {
		return "Test Plugin";
	}
	
	@Override
	public int getPluginVersionCode() {
		return 3;
	}
	
	@Override
	public void init(Properties properties) {		
	}
	
	@Override
	public void setPluginIcon(Object icon) {
		this.icon = icon;
	}
	
	@Override
	public Object getPluginIcon() {
		return icon;
	}
	
	private void parsePage() {
		names = null;
		urls = null;
		types = null;
		descriptions = null;
		thumbsURLs = null;
		
		ArrayList<String> alNames = new ArrayList<>();
		ArrayList<String> alURLs = new ArrayList<>();
		ArrayList<String> alThumbs = new ArrayList<>();
		ArrayList<String> alDescriptions = new ArrayList<>();
		
		Pattern pattern = Pattern.compile("<table>.+?</table>", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(pageText);
		if (matcher.find()) {
			try {
				String table = pageText.substring(matcher.start(), matcher.end());
				
				// Get thumbs URLs
				pattern = Pattern.compile("<img src=\".+?\"", Pattern.CASE_INSENSITIVE);
				matcher = pattern.matcher(table);				
				while (matcher.find()) {
					String thumb = table.substring(matcher.start(), matcher.end());
					alThumbs.add(thumb.substring(10, thumb.indexOf('"', 11)));
				}
				
				// Get videos URLs and names
				pattern = Pattern.compile("<a .+?</a>", Pattern.CASE_INSENSITIVE);
				matcher = pattern.matcher(table);				
				while (matcher.find()) {
					String url = table.substring(matcher.start(), matcher.end());
					alURLs.add(url.substring(9, url.indexOf('"', 9)));
					String name = url.substring(url.indexOf('>') + 1, url.lastIndexOf('<'));
					alNames.add(name);
				}
				
				// Get descriptions
				pattern = Pattern.compile("</a>.+?</td>", Pattern.CASE_INSENSITIVE);
				matcher = pattern.matcher(table);				
				while (matcher.find()) {
					String description = table.substring(matcher.start(), matcher.end());
					description = description.substring(10, description.lastIndexOf('<'));
					description = description.replaceAll("<strong>", "{");
					description = description.replaceAll("</strong>", "}");
					description = description.replaceAll("<br />", "\n");
					alDescriptions.add(description);					
				}				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		if (alNames.size() > 0 && alNames.size() == alURLs.size()) {		
			names = alNames.toArray(new String[alNames.size()]);			
			urls = alURLs.toArray(new String[alURLs.size()]);
			
			types = new int[names.length];
			for (int i = 0;i < types.length;i++) {
				types[i] = LINK_TYPE_FILE;
			}
			
			if (alThumbs.size() == names.length) {
				thumbsURLs = alThumbs.toArray(new String[names.length]);
			}
			
			if (alDescriptions.size() == names.length) {
				descriptions = alDescriptions.toArray(new String[names.length]);
			}
			
			// Getting next page URL
			pattern = Pattern.compile("</table>.+?>Next page</a>", Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(pageText);
			if (matcher.find()) {
				try {
					String nextPage = pageText.substring(matcher.start(), matcher.end());
					int pos = nextPage.indexOf('"');
					nextPageURL = nextPage.substring(pos + 1, nextPage.indexOf('"', pos + 1));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	@Override
	public boolean refresh() {
		try {
			pageText = PageDownloader.downloadPage(currURL, "UTF-8");
			parsePage();
			return true;
		} catch (Exception e) {
			System.err.println("Error reading from URL: " + currURL);
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public String[] getNames() {
		return names;
	}
	
	@Override
	public String[] getURLs() {
		return urls;
	}
	
	@Override
	public int[] getTypes() {
		return types;
	}
	
	@Override
	public String[] getThumbs() {
		return thumbsURLs;
	}
	
	@Override
	public boolean isProvideExtraData() {
		return true;
	}
	
	@Override
	public String getDescription(int itemNum) {
		if (descriptions == null || descriptions.length <= itemNum) return null;
		return descriptions[itemNum];
	}
	
	@Override
	public boolean nextPage() {
		if (nextPageURL == null) return false;

		urlsHistory.add(currURL);
		currURL = nextPageURL;

		boolean res = refresh();
		if (res && urls == null) {
			previousPage();
			return false;
		}
		return res;
	}

	@Override
	public boolean previousPage() {
		if (urlsHistory.size() == 0) return false;

		currURL = urlsHistory.get(urlsHistory.size() - 1);
		urlsHistory.remove(urlsHistory.size() - 1);

		return refresh();
	}

	@Override
	public boolean selectItem(int itemNum) {
		if (urls == null || itemNum < 0 || urls.length <= itemNum ||
				types == null || types[itemNum] == LINK_TYPE_FILE) return false;

		urlsHistory.add(currURL);
		currURL = urls[itemNum];

		return refresh();
	}

	@Override
	public boolean setFilesLink(String[] filesLink) {
		return false;
	}

	@Override
	public String[] getFilesLink(int itemNum) {
		return null;
	}
	
	// Just for plugin local testing
	private void testPlugin() {		
		System.out.println("--- " + getPluginName() + " ---");
		System.out.println("Refresh status: " + refresh() + "\n");
		if (names != null) {
			for (int i = 0;i < names.length;i++) {
				System.out.println("Item num: " + (i + 1));
				System.out.println("Name: " + names[i]);
				System.out.println("URL: " + urls[i]);
				System.out.println("Link type: " + types[i]);
				System.out.println("Thumb URL: " + thumbsURLs[i]);
				System.out.println("Description: " + descriptions[i]);
				System.out.println();
			}
			
			System.out.println("Next page URL: " + nextPageURL);			
		}
	}
	
	// Just for plugin local testing
	public static void main(String[] args) {
		TestPlugin testPlugin = new TestPlugin();		
		testPlugin.testPlugin();
	}
	
}
