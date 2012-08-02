/******************************************************************************
* File:              AFRParser.java
* Author:            Kevin Day
* Date:              March, 2007
* Description:       
*                    
*                    
* Copyright (c) 2005-2007 Kevin Day
* All rights reserved.
*******************************************************************************/

public class AFRParser extends SampleParser {
    public AFRParser()
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
        int adcval = unsignedByte(b[offset+1]) + unsignedByte(b[offset])*256;

        adcval >>= 5;
            
        Double r = 8.0+(adcval/60.0);

        System.out.format("WB ADC %02X %02X = %04X = %f\n", unsignedByte(b[offset]), unsignedByte(b[offset+1]),
                    adcval, r);

        return r;
    }

    public Double getRangeMin()
    {
        return 0.0;
    }
    public Double getRangeMax()
    {
        return 14.7*2;
    }

    public Double getRangeStdMin()
    {
        return 8.0;
    }
    public Double getRangeStdMax()
    {
        return 22.0;
    }
    
}

