/******************************************************************************
* File:              SampleLogXYSeries.java
* Author:            Kevin Day
* Date:              March, 2007
* Description:       
*                    
*                    
* Copyright (c) 2005-2007 Kevin Day
* All rights reserved.
*******************************************************************************/

import org.jfree.data.xy.XYSeries;
import java.util.Iterator;

public class SampleLogXYSeries
{
    public static XYSeries makeXYSeries(SingleAxisSampleLog sl) throws Exception
    {
        XYSeries xys = new XYSeries(sl.GetName());
        populateXYSeries(xys, sl);
        return xys;
    }
    private static void populateXYSeries(XYSeries xys, SingleAxisSampleLog sl) throws Exception
    {        
        Iterator<Sample> i = sl.iterator();

        System.out.print("Creating XY series for log " + sl.GetName() + "\n");
        
        while(i.hasNext())
        {
            Sample s = i.next();
//            System.out.format("%s\n",s.toString());
            s.convertFromRaw();
            xys.add(s.getTime().getDoubleMs(), s.getValue());
//            System.out.print("Added sample " + s.toString() + "\n");
        }
    }
    
}

        
