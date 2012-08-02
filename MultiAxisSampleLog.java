/******************************************************************************
* File:              MultiAxisSampleLog.java
* Author:            Kevin Day
* Date:              March, 2007
* Description:       
*                    
*                    
* Copyright (c) 2005-2007 Kevin Day
* All rights reserved.
*******************************************************************************/

import java.util.TreeMap;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.regex.*;
import java.util.Iterator;
import java.lang.Long;
import org.jfree.data.Range;

public class MultiAxisSampleLog {
    public MultiAxisSampleLog()
    {
    }
    public MultiAxisSampleLog(File f) throws Exception
    {
        LoadFromBinaryFile(f);
    }

    private TreeMap<String, SingleAxisSampleLog> axes = new TreeMap<String, SingleAxisSampleLog>();

    public Iterator<SingleAxisSampleLog> iterator()
    {
        return axes.values().iterator();
    }

    public SingleAxisSampleLog getLogForParameter(String parameterName)
    {
        return axes.get(parameterName);
    }

    public void AddAxis(SingleAxisSampleLog s)
    {
        axes.put(s.GetName(), s);
    }
    public void RenameAxis(String oldname, String newname)
    {
        SingleAxisSampleLog s = axes.get(oldname);
        axes.remove(oldname);
        axes.put(newname, s);
    }
            

    public void LoadFromBinaryFile(File f) throws Exception
    {
        if (!f.canRead())
        {
            throw new Exception("File " + f.getName() + " is not readable");
        }
        
        this.name = f.getName();

        FileInputStream fis = new FileInputStream(f);

        SampleTime lastTime = null;

        while (fis.available() > 0)
        {
            boolean again = false;
            Sample s = null;
            do {
                try {
                    s = new Sample(fis);
                    again = false;
                }
                catch(Exception e)
                {
                    System.out.format("Got exception: %s\n", e.toString());
                    again = true;
                }
//            System.out.format("^^^^^ OKOKOK again: %d\n", again?1:0);
            }
            while (again && fis.available() > 0);
            if (again)
            {
                break;
            }

            if (lastTime != null)
            {
                s.getTime().adjustForWrap(lastTime);
            }
            lastTime = s.getTime();

            SingleAxisSampleLog sl = axes.get(s.GetParameterName());
            if (sl == null)
            {
                sl = new SingleAxisSampleLog(s.GetParameterName());
                axes.put(s.GetParameterName(), sl);
                System.out.print("Added axis " + sl.GetName() + "to multi-axis log\n");
            }
//            System.out.format("****** Add: %s\n", s.toString());
            sl.add(s);
        }
    }

    public void LoadFromAsciiFile(File f) throws Exception
    {
        if (!f.canRead())
        {
            throw new Exception("File " + f.getName() + " is not readable");
        }
        
        this.name = f.getName();

        FileInputStream fis = new FileInputStream(f);
        FileChannel fc = fis.getChannel();
        int sz = (int)fc.size();
        MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, sz);
        CharBuffer cb = decoder.decode(bb);
        
//        pattern = pattern.compile("(\\d+),\\s*([\\d.]+)");
        pattern = pattern.compile("(\\d+),\\s*(\\d+),\\s*([\\d.]+)");
        
        Matcher lm = linePattern.matcher(cb);
        Matcher pm = null;
        int lines = 0;
        while(lm.find()) {
            lines++;
            CharSequence cs = lm.group();
            if (pm == null)
            {
                pm = pattern.matcher(cs);
            }
            else
            {
                pm.reset(cs);
            }
            if (pm.find())
            {
                String parameter    = pm.group(1);
                String usec         = pm.group(2);
                String val          = pm.group(3);
                System.out.print(parameter + ":" + lines + ":" + usec + ":" + val + "\n");

                Sample s = new Sample(Integer.parseInt(parameter), Double.parseDouble(val), new SampleTime(Long.parseLong(usec)));

                SingleAxisSampleLog sl = axes.get(s.GetParameterName());
                if (sl == null)
                {
                    sl = new SingleAxisSampleLog(s.GetParameterName());
                    axes.put(s.GetParameterName(), sl);
                    System.out.print("Added axis " + sl.GetName() + "to multi-axis log\n");
                }
                sl.add(s);
            }
            if (lm.end() == cb.limit())
            {
                break;
            }
        }        
    }

    public Double mapTimeToAxisValue(Double time, String axisname)
    {
        SingleAxisSampleLog sl = axes.get(axisname);
        if (sl == null)
        {
            return 0.0;
        }

        return sl.mapTimeToAxisValue(time);
    }

    public Range GetTimeRange()
    {
        SampleTime min = null;
        SampleTime max = null;
        Iterator<SingleAxisSampleLog> i = iterator();
        while(i.hasNext())
        {
            SingleAxisSampleLog sl = i.next();
            if (min == null ||
                min.greaterThan(sl.getFirstSampleTime()))
            {
                min = sl.getFirstSampleTime();
            }
            if (max == null ||
                sl.getLastSampleTime().greaterThan(max))
            {
                max = sl.getLastSampleTime();
            }
        }
        Range r = new Range(min.getDoubleMs(),max.getDoubleMs());
        return r;
    }



    public String GetName()
    {
        return this.name;
    }
    public void SetName(String name)
    {
        this.name = name;
    }

    private String name;

    private static Charset charset = Charset.forName("ISO-8859-15");
    private static CharsetDecoder decoder = charset.newDecoder();
    private static Pattern linePattern = Pattern.compile(".*\r?\n");
    private static Pattern pattern;
}
