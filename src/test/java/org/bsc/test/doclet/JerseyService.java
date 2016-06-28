/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bsc.test.doclet;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.xml.ws.http.HTTPException;

/**
 *
 * @author softphone
 */
@Path("/test/jersey")
public interface JerseyService {


    /**
     * Service1 Description
     * Testing.......
     *
     *
     * @return 200 OK
     *
     * @see
     * SampleInput
     * {
     *     "input1":"val1",
     *     "input2":"val2",
     *     "input3":"val3",
     *     "input4":"val4",
     *     "input5":"val5",
     *     "input6":"val6"
     * }
     *
     * SampleOutput
     * {
     *     "output1":"oval1",
     *     "output2":"oval2",
     *     "output3":"oval3",
     *     "output4":"oval4",
     *     "output5":"oval5",
     *     "output6":"oval6"
     * }
     *
     */

    @POST
    @Deprecated
    @Path("/testservice1")
    @Produces(MediaType.MULTIPART_FORM_DATA)
    @Consumes(MediaType.APPLICATION_JSON)
    public Long service1(@QueryParam("param1") String param1, POJOSample pojoSample, int paramTesting) throws HTTPException, Exception;


    /**
     * Service2 Description
     * Testing.......
     *
     * @return 200 OK
     * @throws 500 403
     * @see "SampleInput:
     * {
     * "Test":"Testing"
     * }
     * <p/>
     * SampleOutput:
     * {
     * "Output":"Output"
     * }"
     * @deprecated
     */

    @POST
    @Deprecated
    @Path("/testservice2/{param1}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public POJOSample serviceTest2(@QueryParam("qparam") String qparam, POJOSample pojoSample, @PathParam("param1") Long param1);

    @GET
    @Path("/testservice2emptyInpt")
    void serviceTest2();
}
