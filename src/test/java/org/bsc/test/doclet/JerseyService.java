/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bsc.test.doclet;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 *
 * @author softphone
 */
@Path( "test")
public class JerseyService {
    
    
    /**
     * Test service
     * 
     * 
     * @param param1 Test param
     * @param enabled True|False
     * @since 1.0
     * @deprecated  use service2 instead
     * @return return generic string
     */
    @GET
    @Deprecated
    public String service1( String param1, boolean enabled )
    {
        return null;
    }
    
}
