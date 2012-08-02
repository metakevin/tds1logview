/******************************************************************************
* File:              Sample.java
* Author:            Kevin Day
* Date:              March, 2007
* Description:       
*                    
*                    
* Copyright (c) 2005-2007 Kevin Day
* All rights reserved.
*******************************************************************************/

import java.lang.Comparable;
import java.io.FileInputStream;
import java.nio.channels.*;

public class Sample implements Comparable {
    static int maxtypes;
    static SampleType[] types;    
    SampleParser defaultParser = new SampleParser();

    static {
        maxtypes = 20;
        types = new SampleType[maxtypes];

        types[1]  = new SampleType("MAP PSIS", new MAPParser());
        types[3]  = new SampleType("IAT Deg. C", new IATParser());
        types[4]  = new SampleType("Volts", new VoltParser());
        types[5]  = new SampleType("MPH", new SampleParser());
        types[6]  = new SampleType("RPM", new SampleParser());
        types[8]  = new SampleType("EGT", new EGTParser());
        types[9]  = new SampleType("Air fuel ratio", new AFRParser());
        types[10] = new SampleType("Fuel Pressure PSIS", new FuelParser());        
    }

    public boolean typeIsValid(int type)
    {
        if (type < maxtypes && types[type] != null)
        {
            return true;
        }
        return false;
    }

    public Sample(int parameter, Double value, SampleTime time)
    {
        this.p = parameter;
        this.v = value;
        this.time = time;
    }

    public Sample(FileInputStream fis) throws Exception
    {
        readFrom(fis);
    }

    private void readFrom(FileInputStream fis) throws Exception
    {
        int flen = fis.read();
        int flags = flen&0xF;
        int length = flen>>4;
        System.out.format("Sample: %02X Length %X flags %X\n", flen, length, flags);

        if (flags != 0xA || length < 2 || length > 4)
        {
            //throw new Exception("malformed sample: missing flags");

        
            int maxlen = 4;
            int hdrlen = 6;


            FileChannel fc = fis.getChannel();
            int skip = 0;
            do {
                long pos = fc.position();
                byte buf[] = new byte[maxlen + hdrlen + 1];
                fis.read(buf);

                fc.position(pos + 1);

                flen = ((int)buf[0])&0xFF;
                flags = flen&0xF;
                length = flen>>4;

                if (flags == 0xA && length<=4 && (buf[hdrlen + length]&0xF)==0xA)
                {
                    System.out.format("\rResync input: skipped %d bytes\n", skip);
                    break;
                }
                ++skip;
                System.out.format("[skip %d]\r", skip);
            } while (fis.available() > 0);
        }

        this.p  = fis.read();

        byte tsbytes[] = new byte[4];
        int r = fis.read(tsbytes);
        if (r != 4)
        {
            throw new Exception ("Incomplete timestamp");
        }        
        this.time = new SampleTime(tsbytes);

//        System.out.format(" Param %02X time %s ", this.p, this.time.toString());


        byte sdata[] = new byte[length];
        r = fis.read(sdata);
        if (r != length)
        {
            System.out.format("!!!! length error: %d %d\n", r, length);            
            throw new Exception ("sample data missing");
        }
        this.raw = sdata;
        //convertFromRaw();            
    }

    public void convertFromRaw() throws Exception
    {
        if (typeIsValid(this.p))
        {
            this.v = types[this.p].getParser().fromBytes(this.raw);
//            System.out.format("%%%% OK\n");
        }
        else
        {
            System.out.format("!!!! type  error: %d\n", this.p);            
            throw new Exception ("bad type");
        }
    }

    public SampleParser getParser()
    {
        return types[this.p].getParser();
    }

    public Double getValue()
    {
        if (this.p < maxtypes && types[this.p] != null)
        {
            return types[this.p].getParser().fromDouble(this.v);
        }
        else
        {
            return this.v;
        }
    }

    public void setValue(Double v)
    {
        this.v = v;
    }
    
    public SampleTime getTime()
    {
        return this.time;
    }

    public int compareTo(Object o) throws ClassCastException 
    {
        if (!(o instanceof Sample))
        {
            throw new ClassCastException("Sample::compareTo: Sample object expected");
        }
        Sample s = (Sample)o;
        if (this.p != s.p)
        {
            return this.p - s.p;
        }
        return (int) (this.time.GetMicroseconds() - s.time.GetMicroseconds());
    }    

    public String toString()
    {
        return this.time.toString() + " " + this.GetParameterName() + " " + this.v.toString();
    }
    
    public String GetParameterName()
    {
        return parameterToString(this.p);
    }

    private String parameterToString(int param)
    {
        if (param < maxtypes && types[param] != null)
        {
            return types[param].getName();
        }
        else
        {
            return "Unknown " + param + "?";
        }
    }

    
    private int p;
    private Double v;
    private SampleTime time;

    private byte[] raw;
}

    
