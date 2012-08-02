/******************************************************************************
* File:              IATParser.java
* Author:            Kevin Day
* Date:              March, 2007
* Description:       
*                    
*                    
* Copyright (c) 2005-2007 Kevin Day
* All rights reserved.
*******************************************************************************/

public class IATParser extends SampleParser {
    public IATParser()
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


        /* convert as little endian 16 bit value 
         * representing 5/65536 volt per LSB */
        int adcval = unsignedByte(b[0]) + unsignedByte(b[1])*256;

        Double Vadc = new Double(adcval) * 5.0 / 65536.0;

        Double Rtherm = (Vadc * 2342.0) / (5.0 - Vadc);
        
        Double TempC = (Rtherm - 456) / 1.62;

        System.out.format("IAT %02X %02X -> %04X -> %f ---> %f = %f\n", 
                b[0], b[1], adcval, Vadc, Rtherm, TempC);
        return TempC;
    }

    public Double getRangeMin()
    {
        return -50.0;
    }
    public Double getRangeMax()
    {
        return 500.0;
    }

}

