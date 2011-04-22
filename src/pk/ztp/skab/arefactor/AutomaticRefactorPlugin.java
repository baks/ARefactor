package pk.ztp.skab.arefactor;

import java.util.Properties;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class AutomaticRefactorPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "ARefactor";

	// The shared instance
	private static AutomaticRefactorPlugin plugin;
	
	/**
	 * The constructor
	 */
	public AutomaticRefactorPlugin() 
	{
		//PropertyConfigurator.configure(CreateLog4JProperties());
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception 
	{
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception 
	{
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static AutomaticRefactorPlugin getDefault() 
	{
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) 
	{
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
	
	private Properties CreateLog4JProperties()
	{
		Properties p=new Properties();
		p.put("log4j.rootLogger","debug, R");
		p.put("log4j.appender.R","org.apache.log4j.FileAppender");
		p.put("log4j.appender.R.File","C:\\example.log");
		p.put("log4j.appender.R.layout","org.apache.log4j.PatternLayout");
		p.put("log4j.appender.R.layout.ConversionPattern","%p %t %c - %m%n");
		return p;
	}
	
}
