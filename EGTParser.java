/******************************************************************************
* File:              EGTParser.java
* Author:            Kevin Day
* Date:              March, 2007
* Description:       
*                    
*                    
* Copyright (c) 2005-2007 Kevin Day
* All rights reserved.
*******************************************************************************/

public class EGTParser extends SampleParser {
    public EGTParser()
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
        if (b.length < 4)
        {
            return 0.0;
        }
        
        if (b[2] < 0)
        {
            /* negative */
            return 0.0;
        }

        int tcvolts = unsignedByte(b[2])*(2*2*2*2*2) + unsignedByte(b[3])/(2*2*2);
//        int coldC   = unsignedByte(b[0]);
        int coldC = 0;            
        Double r = coldC + ((tcvolts*3)>>3) + 5.0;

//        int tcvolts = unsignedByte(b[2])*256 + unsignedByte(b[3]);
//        int coldC = 0;
//        Double r = tcvolts * 1.0;

        System.out.format("EGT ADC %02X %02X %02X %02X = %04X + %02X = %f\n", 
                unsignedByte(b[0]), unsignedByte(b[1]),
                unsignedByte(b[2]), unsignedByte(b[3]),
                    tcvolts, coldC, r);

        return r;
    }

    public Double getRangeMin()
    {
        return 0.0;
    }
    public Double getRangeMax()
    {
        return 1000.0;
    }
}

