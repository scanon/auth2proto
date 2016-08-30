package us.kbase.auth2.service.api;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.glassfish.jersey.server.mvc.Template;

import com.google.common.collect.ImmutableMap;

import us.kbase.auth2.lib.AuthUser;
import us.kbase.auth2.lib.Authentication;
import us.kbase.auth2.lib.Password;
import us.kbase.auth2.lib.Role;
import us.kbase.auth2.lib.UserName;
import us.kbase.auth2.lib.exceptions.MissingParameterException;
import us.kbase.auth2.lib.exceptions.NoSuchUserException;
import us.kbase.auth2.lib.exceptions.UserExistsException;
import us.kbase.auth2.lib.storage.exceptions.AuthStorageException;
import us.kbase.auth2.lib.token.IncomingToken;

@Path("/admin")
public class Admin {

	//TODO TEST
	//TODO JAVADOC

	//TODO ADMIN reset user pwd
	//TODO NOW find user
	
	@Inject
	private Authentication auth;
	
	@GET
	public String admin() {
		return "foo";
	}
	
	@GET
	@Path("/localaccount")
	@Template(name = "/adminlocalaccount")
	@Produces(MediaType.TEXT_HTML)
	public Map<String, String> createLocalAccountStart(
			@QueryParam("admin") final String adminName) {
		//TODO ADMIN get adminName from token
		return ImmutableMap.of("name", adminName,
				"targeturl", "/admin/localaccount/create");
	}
	
	@POST
	@Path("/localaccount/create")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.TEXT_HTML)
	@Template(name = "/adminlocalaccountcreated")
	public Map<String, String> createLocalAccountComplete(
			@FormParam("user") final String userName,
			@FormParam("full") final String fullName,
			@FormParam("email") final String email)
			throws AuthStorageException, UserExistsException,
			MissingParameterException {
		//TODO ADMIN check user is admin
		//TODO NOW log
		//TODO NOW email class with proper checking (probably not validation)
		if (userName == null) {
			throw new MissingParameterException("userName");
		}
		final Password pwd = auth.createLocalUser(
				new UserName(userName), fullName, email);
		final Map<String, String> ret = ImmutableMap.of(
				"user", userName,
				"full", fullName,
				"email", email,
				"password", new String(pwd.getPassword())); // char[] won't work
		pwd.clear(); // not that this helps much...
		return ret;
	}
	
	@GET
	@Path("/user/{user}")
	@Template(name = "/adminuser")
	@Produces(MediaType.TEXT_HTML)
	public Map<String, Object> userDisplay(
			@PathParam("user") final String user)
		throws AuthStorageException, NoSuchUserException {
		//TODO ADMIN get adminname from token & check
		final AuthUser au = auth.getUserAsAdmin(
				new IncomingToken("fake"), new UserName(user));
		final Map<String, Object> ret = new HashMap<>();
		ret.put("roleurl", "/admin/user/" + user + "/roles");
		ret.put("user", au.getUserName().getName());
		ret.put("full", au.getFullName());
		ret.put("email", au.getEmail());
		ret.put("local", au.isLocal());
		final List<Role> r = au.getRoles();
		//TODO ADMIN only show admin button if root user
		//TODO ADMIN only allow changing admin status if root user
		ret.put("admin", Role.hasRole(r, Role.ADMIN));
		ret.put("serv", Role.hasRole(r, Role.SERV_TOKEN));
		ret.put("dev", Role.hasRole(r, Role.DEV_TOKEN));
		ret.put("custom", au.getCustomRoles());
		return ret;
	}

	@POST
	@Path("/user/{user}/roles")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public void changeRoles(
			@PathParam("user") final String user,
			final MultivaluedMap<String, String> form)
			throws NoSuchUserException, AuthStorageException {
		//TODO ADMIN get adminname from token & check
		final List<Role> roles = new LinkedList<>();
		addRoleFromForm(form, roles, "admin", Role.ADMIN);
		addRoleFromForm(form, roles, "dev", Role.DEV_TOKEN);
		addRoleFromForm(form, roles, "serv", Role.SERV_TOKEN);
		auth.updateRoles(new IncomingToken("fake"), new UserName(user), roles);
		
	}

	private void addRoleFromForm(
			final MultivaluedMap<String, String> form,
			final List<Role> roles,
			final String rstr,
			final Role role) {
		if (form.get(rstr) != null) {
			roles.add(role);
		}
	}

}
