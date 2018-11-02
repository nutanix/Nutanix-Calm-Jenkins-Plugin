package com.calm.Logger;


import java.util.logging.Level;
import java.util.logging.Logger;

public class NutanixCalmLogger{
    private Logger logger;
    public  NutanixCalmLogger(Class clazz){
        this.logger = Logger.getLogger(clazz.getName());
    }

    public void debug(String message){
        logger.log(Level.SEVERE, message);
    }

    public void info(String message){
        logger.info(message);
    }

    public String getStackTraceStr(StackTraceElement[] stackTraceElements){
        String stackStr = "";
        for(StackTraceElement stackTraceElement : stackTraceElements)
            stackStr += stackTraceElement.toString();
        return stackStr;
    }
}
