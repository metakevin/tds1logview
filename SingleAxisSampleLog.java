/******************************************************************************
* File:              SingleAxisSampleLog.java
* Author:            Kevin Day
* Date:              March, 2007
* Description:       
*                    
*                    
* Copyright (c) 2005-2007 Kevin Day
* All rights reserved.
*******************************************************************************/

import java.util.TreeSet;
import java.util.ArrayList;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.regex.*;
import java.util.Iterator;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.axis.NumberAxis;

//public class SingleAxisSampleLog extends TreeSet<Sample>{    
public class SingleAxisSampleLog extends ArrayList<Sample>{
    public SingleAxisSampleLog()
    {
        SetName("no name");
    }
    public SingleAxisSampleLog(String name)
    {
        SetName(name);
    }
    public SingleAxisSampleLog(SingleAxisSampleLog other)
    {
        Iterator<Sample> i = other.iterator();
        while(i.hasNext())
        {
            this.add(i.next());
        }
    }

    private SampleTime minTime;
    private SampleTime maxTime;
    public boolean add(Sample s)
    {
        if (this.minTime == null ||
            this.minTime.greaterThan(s.getTime()))
        {
            this.minTime = s.getTime();
        }
        if (this.maxTime == null ||
            s.getTime().greaterThan(this.maxTime))
        {
            this.maxTime = s.getTime();
        }
        
        return super.add(s);
    }

    public SampleTime getFirstSampleTime()
    {
        return minTime;
    }
    public SampleTime getLastSampleTime()
    {
        return maxTime;
    }


    public Iterator<Sample> iterator()
    {
        return super.iterator();
    }

    public String GetName()
    {
        return this.name;
    }
    public void SetName(String name)
    {
        this.name = name;
    }

    public void setRenderer(XYLineAndShapeRenderer rend)
    {
        this.renderer = rend;
    }
    public XYLineAndShapeRenderer getRenderer()
    {
        return this.renderer;        
    }
    public void setAxis(NumberAxis a)
    {
        this.axis = a;
    }
    public NumberAxis getAxis()
    {
        return this.axis;
    }
    private String name;
    private XYLineAndShapeRenderer renderer;
    private NumberAxis axis;
    private TDSLogViewer.LogTreeNode lognode;
    private int dataset;

    public int getDataset()
    {
        return dataset;
    }
    public void setDataset(int d)
    {
        dataset = d;
    }

    public SampleParser getParser()
    {
        return super.get(0).getParser();
    }

    public TDSLogViewer.LogTreeNode getLogTreeNode()
    {
        return lognode;
    }
    public void setLogTreeNode(TDSLogViewer.LogTreeNode n)
    {
        lognode = n;
    }

    public void deGlitch()
    {
        int i;

        for(i=1; i<super.size()-1; i++)
        {
            Sample p = super.get(i-1);
            Sample c = super.get(i);
            Sample n = super.get(i+1);
            Double pchg = c.getValue()-p.getValue();
            Double nchg = n.getValue()-c.getValue();
            Double pnchg = p.getValue() - n.getValue();

            System.out.format("Test: %f %f %f\n", pchg, nchg, pnchg);
            if (Math.abs(pchg) > Math.abs(pnchg) && Math.abs(nchg) > Math.abs(pnchg))
            {
                Double deglitch = (p.getValue()+n.getValue())/2;
                System.out.format("Glitch trim: %f %f %f -> %f %f %f\n",
                        p.getValue(), c.getValue(), n.getValue(), p.getValue(), deglitch, n.getValue());
                c.setValue(deglitch);
            }
        }
    }

    public void lowpassFilter(Double f_cutoff)  // rc * timedelta ; 0<rc<1
    {
        Double rc;

        // f_cutoff = 1/(2piRC)
        // f_cutoff*2piRC = 1
        // 2piRC = 1/f_cutoff

        Double RCpi2 = 1.0/f_cutoff;

        Double RC = RCpi2/(3.14159265359*2);
        
        int i;

        for(i=1; i<super.size()-1; i++)
        {
            Sample p = super.get(i-1);
            Sample c = super.get(i);

            Double Tdelt = (c.getTime().getDoubleMs()-p.getTime().getDoubleMs())/1000;

            Double alpha = Tdelt/(Tdelt+RC);

            Double filt = p.getValue()*(1-alpha) + c.getValue()*alpha;
            System.out.format("LPF @ %2.1f: %2.2f --> %2.2f (alpha = %2.2f RC = %2.2f)\n", f_cutoff, c.getValue(), filt, alpha, RC);
            c.setValue(filt);
        }
    }

    public Double mapTimeToAxisValue(Double time)
    {
        /* slow... */
        int i;
        for(i=1; i<super.size()-1; i++)
        {
            Sample p = super.get(i-1);
            Sample c = super.get(i);
            Double Tp = p.getTime().getDoubleMs();
            Double Tc = c.getTime().getDoubleMs();            
            if (Tp <= time && Tc >= time)
            {
                Double Tinterval = Tc-Tp;
                Double Tinterp = Tc-time;
                Double Fc = (Tinterval-Tinterp)/Tinterval;
                Double Fp = 1.0-Fc;

                Double v = Fc*c.getValue() + Fp*p.getValue();

                System.out.format("Time %f: Tp %f Tc %f Tinterval %f Tinterp %f Fc %f Fp %f v %f\n",
                        time, Tp, Tc, Tinterval, Tinterp, Fc, Fp, v);

                return v;
            }
        }
        System.out.format("mapTimeToAxisValue(%f): super.size=%d no match\n", time, super.size());
        return 0.0;
    }




   
}
