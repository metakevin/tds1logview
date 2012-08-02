/******************************************************************************
* File:              FuelParser.java
* Author:            Kevin Day
* Date:              March, 2007
* Description:       
*                    
*                    
* Copyright (c) 2005-2007 Kevin Day
* All rights reserved.
*******************************************************************************/

public class FuelParser extends SampleParser {
    public FuelParser()
    {
    }

    public Double fromDouble(Double rawsample)
    {
        return rawsample;
    }

    public int unsignedByte(byte b)
    {
        int r = b;
        r &= 0xFF;
        return r;
    }

    public Double fromBytes(byte b[])
    {
        if (b.length < 2)
        {
            return 0.0;
        }

        int offset = b.length - 2;


        /* convert as little endian 16 bit value 
         * representing 5/65536 volt per LSB */
        int adcval = unsignedByte(b[offset]) + unsignedByte(b[offset+1])*256;

        Double r = new Double(adcval) * 5 / 65536;
        //Double r = new Double(adcval) * 5 / 8192;

        //System.out.format("Raw %02X %02X Value: %04X / %1.3f\n", b[offset], b[offset+1], adcval&0xFFFF, r);

        return ((r - 0.5)/4)*150;
    }

    public Double getRangeMin()
    {
        return 0.0;
    }
    public Double getRangeMax()
    {
        return 150.0;
    }
    public Double getRangeStdMin()
    {
        return 43.5;
    }
    public Double getRangeStdMax()
    {
        return 87.0;
    }


}

