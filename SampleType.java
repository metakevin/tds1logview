/******************************************************************************
* File:              SampleType.java
* Author:            Kevin Day
* Date:              March, 2007
* Description:       
*                    
*                    
* Copyright (c) 2005-2007 Kevin Day
* All rights reserved.
*******************************************************************************/

public class SampleType {
    public SampleType(String typeName, SampleParser typeParser)
    {
        this.name = typeName;
        this.parser = typeParser;
    }

    public String getName()
    {
        if (parser != null && parser.getAdditionalText().length() > 0)
        {
            return this.name + " " + parser.getAdditionalText();
        }
        else
        {
            return this.name;
        }
    }

    public SampleParser getParser()
    {
        return this.parser;

    }
    private String name;
    private SampleParser parser;
}
