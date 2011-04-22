package pk.ztp.skab.arefactor.Logger;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import pk.ztp.skab.arefactor.popup.actions.CreationMethodsAction;

public class ARefactorLogger 
{
    private volatile static Logger logger;
	private static FileHandler fileTxt;
	private static SimpleFormatter formatterTxt;
    
    private ARefactorLogger()
    {
    }
 
    public static Logger getInstance()
    {
        if (logger == null) 
        {
            synchronized (ARefactorLogger.class) 
            {
                if (logger == null) 
                {
                    prepareLogger();
                }
                return logger;
            }
        } 
        else 
        {
            return logger;
        }
    }
    
    private static void prepareLogger()
    {
    	logger=Logger.getLogger(ARefactorLogger.class.getName());
    	logger.setLevel(Level.ALL);
    	try 
		{
			fileTxt = new FileHandler("C:\\Logging.txt");
		} 
		catch (SecurityException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		formatterTxt = new SimpleFormatter();
		fileTxt.setFormatter(formatterTxt);
		logger.addHandler(fileTxt);
    }
}
