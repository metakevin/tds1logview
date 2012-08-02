/******************************************************************************
* File:              SampleTime.java
* Author:            Kevin Day
* Date:              March, 2007
* Description:       
*                    
*                    
* Copyright (c) 2005-2007 Kevin Day
* All rights reserved.
*******************************************************************************/

public class SampleTime {

    static Long SampleCounter = new Long(0);

    public SampleTime(Long microseconds)
    {
        this.us = microseconds;
    }

    public SampleTime(byte raw[]) throws Exception
    {
        long t;
        t =  unsignedByte(raw[0]);
        t += unsignedByte(raw[1])<<8;
        t += unsignedByte(raw[2])<<16;
        t += unsignedByte(raw[3])<<24;

//        System.out.format("[raw time: %02X %02X %02X %02X = %08X\n", raw[0], raw[1], raw[2], raw[3], t);

        this.us = t;

        if (this.us > 65536000L)
        {
            throw new Exception("Invalid timestamp");
        }

//        this.us = SampleCounter++;
    }   

    public void adjustForWrap(SampleTime prev)
    {
        /* The timestamp format used on the AVR is in units of microseconds,
         * but it does not wrap on a 32 bit boundary.
         * Instead it is (milliseconds*1000)+delta
         * milliseconds wraps on 16 bits.
         * So the timestamp wraps every 65536000 microseconds (= 65 seconds) */
        System.out.format("Timestamp: was %20d ", this.us);

        Long prevtotal = prev.GetMicroseconds()/1000;
        Long prev_upper = prevtotal&(~0xFFFF);
        Long prev_add = prev_upper*1000;
        this.us += prev_add;

        if (this.us < prev.GetMicroseconds() - 32767000)
        {
            this.us += 65536000L;
        }

        System.out.format(" now %20d\n", this.us);
    }

    private long unsignedByte(byte b)
    {
        long r = b;
        r &= 0xFF;
        return r;
    }
 
    public Long GetMicroseconds()
    {
        return this.us;
    }

    public Long GetMilliseconds()
    {
        return this.us/1000;
    }
    
    public Double getDoubleMs()
    {
        return this.us/1000.0;
    }

    public String toString()
    {
        return this.us.toString();
    }
    
    private Long us;

    public boolean greaterThan(SampleTime other)
    {
        if (us > other.us)
        {
            return true;
        }
        else
        {
            return false;
        }
    }
}
