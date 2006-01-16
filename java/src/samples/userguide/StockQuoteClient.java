package samples.userguide;

import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.MessageContextConstants;
import org.apache.axis2.om.*;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.HttpTransportProperties;
import org.apache.axis2.transport.http.HttpTransportProperties.ProxyProperties;

import javax.xml.namespace.QName;

public class StockQuoteClient {

    /**
     * @param args <p/>
     *             This is a fairly static test client for Synapse. It makes a
     *             StockQuote request to WebServiceX stockquote service. The EPR it
     *             is sent to is for WebServiceX, but the actual transport URL is
     *             designed to go to the Synapse listener.
     */
    public static void main(String[] args) {

        if (args.length > 0 && args[0].substring(0, 1).equals("-")) {
            System.out
                    .println("Usage: StockQuoteClient Symbol StockQuoteURL TransportURL");
            System.out
                    .println("\nDefault values: IBM http://www.webservicex.net/stockquote.asmx http://localhost:8080");
            System.out
                    .println("\nThe XMethods URL will be used in the <wsa:To> header");
            System.out
                    .println("The Transport URL will be used as the actual address to send to");
            System.out
                    .println("\nTo bypass Synapse, set the transport URL to the WebServiceX URL: \n"
                            + "e.g. StockQuoteClient IBM http://www.webservicex.net/stockquote.asmx  http://www.webservicex.net/stockquote.asmx \n"
                            + "\nTo demonstrate Synapse virtual URLs, set the URL to http://stockquote\n"
                            + "\nTo demonstrate content-based behaviour, set the Symbol to MSFT\n"
                            + "\nAll examples depend on using the sample synapse.xml");
            System.exit(0);
        }

        String symb = "IBM";
        String xurl = "http://www.webservicex.net/stockquote.asmx";
        String turl = "http://localhost:8080";

        if (args.length > 0)
            symb = args[0];
        if (args.length > 1)
            xurl = args[1];
        if (args.length > 2)
            turl = args[2];

        try {

            // step 1 - create a request payload
            OMElement getQuote = StockQuoteXMLHandler
                    .createRequestPayload(symb);

            // step 2 - set up the call object

            // the wsa:To
            EndpointReference targetEPR = new EndpointReference(xurl);

            Options options = new Options();
            //options.setProperty(MessageContextConstants.TRANSPORT_URL, turl);
            options.setTo(targetEPR);


            options.setAction("http://www.webserviceX.NET/GetQuote");
            options.setSoapAction("http://www.webserviceX.NET/GetQuote");

            // options.setProperty(MessageContextConstants.CHUNKED, Constants.VALUE_FALSE);
            ServiceClient serviceClient = new ServiceClient();

            serviceClient.setOptions(options);

            // step 3 - Blocking invocation
            OMElement result = serviceClient.sendReceive(new QName("getQuote"),
                    getQuote);
            // System.out.println(result);

            // step 4 - parse result

            System.out.println("Stock price = $"
                    + StockQuoteXMLHandler.parseResponse(result));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}