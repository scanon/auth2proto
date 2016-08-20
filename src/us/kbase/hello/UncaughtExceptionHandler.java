package us.kbase.hello;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;


public class UncaughtExceptionHandler implements ExceptionMapper<Throwable> {

	//TODO AUTH make an application wide exception for handling known errors
	
	@Context
	private HttpHeaders headers;
	private final ObjectMapper mapper = new ObjectMapper(); 
	
	@Override
	public Response toResponse(Throwable ex) {

		final MediaType mt = getMediaType();

		final ErrorMessage em = getError(ex);
		String ret;
		if (mt.equals(MediaType.APPLICATION_JSON_TYPE)) {
			try {
				ret = mapper.writeValueAsString(em);
			} catch (JsonProcessingException e) {
				ret = "An error occured in the error handler when " +
						"processing the error object to JSON. " +
						"This shouldn't happen.";
				LoggerFactory.getLogger(getClass()).error(ret, e);
			}
		} else {
			//TODO AUTH mustach template locs should be set the same way as in the app init
			final MustacheFactory mf = new DefaultMustacheFactory(
					new File("./webapps/templates")); //TODO NOW this won't work for jetty
			final Mustache mus = mf.compile("uncaughtexception.mustache");
			final StringWriter wr = new StringWriter();
			mus.execute(wr, em);
			ret = wr.toString();
		}
		LoggerFactory.getLogger(getClass()).error("Uncaught exception", ex);
		
		return Response.status(em.getCode())
				.entity(ret)
				.type(mt)
				.build();
	}

	// either html or json
	private MediaType getMediaType() {
		MediaType mt = null;
		//sorted by q-value
		final List<MediaType> mtypes = headers.getAcceptableMediaTypes();
		if (mtypes != null) {
			for (final MediaType m: mtypes) {
				if (m.equals(MediaType.TEXT_HTML_TYPE) ||
						m.equals(MediaType.APPLICATION_JSON_TYPE)) {
					mt = m;
					break;
				}
			}
		}
		if (mt == null) {
			mt = MediaType.TEXT_HTML_TYPE;
		}
		return mt;
	}
	
	private ErrorMessage getError(final Throwable ex) {
		final StringWriter st = new StringWriter();
		ex.printStackTrace(new PrintWriter(st));
		//TODO AUTH remove substring
//		final String stack = st.toString();
		final String stack = st.toString().substring(0, 100);
		if (ex instanceof WebApplicationException) {
			final Response res = ((WebApplicationException) ex).getResponse();
			//TODO AUTH only return exception in debug mode
			return new ErrorMessage(res.getStatus(),
					res.getStatusInfo().getReasonPhrase(), ex.getMessage(),
					stack);
		} else {
			//defaults to internal server error 500
			return new ErrorMessage(
					Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
					Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase(),
					ex.getMessage(),
					stack);
		}
	}
}
