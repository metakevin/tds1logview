/******************************************************************************
* File:              MAPParser.java
* Author:            Kevin Day
* Date:              March, 2007
* Description:       
*                    
*                    
* Copyright (c) 2005-2007 Kevin Day
* All rights reserved.
*******************************************************************************/

public class MAPParser extends SampleParser {
    public interface Transducer {
        public Double adcToPSI(int adc);
        public String getName();
    } 

    public class BoschTransducer implements Transducer {
        public BoschTransducer(String name, int code_bar, int code_psi, int code_inhg, int offset)
        {
            this.name      = name;
            this.offset    = offset;
            this.code_psi  = code_psi;
            this.code_bar  = code_bar;
            this.code_inhg = code_inhg;
        }

        public Double adcToPSI(int adc)
        {
            if (adc > this.offset)
            {
                adc += this.offset;
            }

            Double psia = new Double(adc) / new Double(this.code_psi);

            System.out.format("Transducer %s: %04X -> %f\n", name, adc, psia);

            return psia - 14.7;
        }

        public String getName()
        {
            return name;
        }

        String name;
        int    offset;
        int    code_psi;
        int    code_bar;
        int    code_inhg;
    }

    Transducer[] transducers;
    int          current_transducer;

    public MAPParser()
    {
        int max_transducers = 10;
        int i = 0;

        transducers = new Transducer[max_transducers];

        transducers[i++] = new BoschTransducer("Bosch 3.0", 303, 1338, 657, -1344);
        transducers[i++] = new BoschTransducer("Bosch 2.5", 409, 1807, 887,  2752);
        transducers[i++] = new BoschTransducer("Bosch 2.6", 382, 1614, 793,  1706);
        transducers[i++] = new BoschTransducer("Bosch 2.7", 359, 1554, 763,  800 );
        transducers[i++] = new BoschTransducer("Bosch 2.8", 338, 1499, 736, -1   );
        transducers[i++] = new BoschTransducer("Bosch 2.9", 320, 1447, 711, -714 );
        transducers[i++] = new BoschTransducer("Bosch 3.1", 288, 1354, 665, -1930);
        transducers[i++] = new BoschTransducer("Bosch 3.2", 275, 1311, 644, -2453);
        transducers[i++] = new BoschTransducer("Bosch 2.0", 476, 2098, 1031, 0   );


        current_transducer = 0;
    }

    public String getAdditionalText()
    {
        return transducers[current_transducer].getName();
    }
    
    public boolean hasParameters()
    {
        return true;
    }

    public void setParameter(int p)
    {
        ++current_transducer;
        System.out.format("Current transducer now %d (%s)\n", current_transducer,
                getAdditionalText());
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
        int adc = unsignedByte(b[1])*256 + unsignedByte(b[0]);
 
        return transducers[current_transducer].adcToPSI(adc);
    }

    public Double getRangeMin()
    {
        return -14.5;
    }
    public Double getRangeMax()
    {
        return 29.0;
    }
    public Double getRangeStdMin()
    {
        return transducers[current_transducer].adcToPSI(0);
    }
    public Double getRangeStdMax()
    {
        return transducers[current_transducer].adcToPSI(65535);
    }
}

