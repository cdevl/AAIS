package com.db.MT.Framework.AAISServiceCo;
import java.util.*;
import java.io.*;
//import com.db.MT.Framework.AAISRMIServerCo.Emo;
import com.db.MT.Framework.EventCo.*;


//first change
//q1 change
public class AAISApplication {

 protected AAISIdentity    m_identity    = new AAISIdentity();
 protected AAISAccessToken m_accessToken = new AAISAccessToken();

 protected String          m_applicationName;

 public static String AAISApplicationName="AAIS";

 public AAISApplication() throws Event{
   AAISIdentity identity = new AAISIdentity();
   mf_init( AAISApplication.AAISApplicationName, identity );
 }

/** <p>Server side constructor. The serviceInfo is a string delimited by "|"
  * that has the following format: serviceName+"|"+adminUser+"|"+adminPassword
  * The serviceName is in the form serviceName@host . Calling AAISIdentity with
  * serviceInfo will create the signature of adminUser (Elara) and
  * contxt_token (cybersafe) of the serviceName.
  * </p>
  */
 public AAISApplication(String applicationName,String serviceInfo) throws Event{
    ReadProp rP = new ReadProp();
    String enabledElara = rP.getAppnameInfo(applicationName,"ELARA_ENABLED");
    if (enabledElara==null ||  enabledElara.equalsIgnoreCase("YES") ) {
       Event.trace("Elara is enabled for "+applicationName+ " "+serviceInfo, "", "LOGIN" );
       AAISIdentity identity = new AAISIdentity(serviceInfo);
       mf_init( applicationName, identity );
    } else if ( enabledElara.equalsIgnoreCase("NO") ) {
       Event.trace("Elara is disabled for "+applicationName+ " "+serviceInfo, "", "LOGIN" );
       AAISIdentity identity = new AAISIdentity(serviceInfo,false);
       mf_init( applicationName, identity );
    }
 }

 public AAISApplication(String applicationName,AAISIdentity identity) throws Event {
   mf_init( applicationName, identity );
 }

/** <p>Client side constructor. </p>
  */
 public AAISApplication(String applicationName,String username, String password) throws Event{
    ReadProp rP = new ReadProp();
    String enabledElara = rP.getAppnameInfo(applicationName,"ELARA_ENABLED");
    if (enabledElara==null ||  enabledElara.equalsIgnoreCase("YES") ) {
       Event.trace("Elara is enabled for "+username, "", "LOGIN" );
       AAISIdentity identity = new AAISIdentity(username,password);
       mf_init( applicationName, identity );
    } else if ( enabledElara.equalsIgnoreCase("NO") ) {
       Event.trace("Elara is disabled for "+username, "", "LOGIN" );
       AAISIdentity identity = new AAISIdentity(username,password,false);
       mf_init( applicationName, identity );
    }
 }

 public AAISApplication(String applicationName,String certificateFileName,
                    String certificateName, String passPhrase ) throws Event {
   AAISIdentity identity = new AAISIdentity(certificateFileName,
       		                       certificateName,passPhrase);
   mf_init( applicationName, identity );
 }

 public String getApplicationName()      { return m_applicationName; }

 public AAISIdentity getIdentity()       { return m_identity; }

 public AAISAccessToken getAccessToken() { return m_accessToken; }

 public boolean verifyClient(AAISAccessToken accessToken) throws Event{
   return accessToken.verify(m_applicationName, m_identity.getCredentials());
 }

/**  <p> This method is used for two purposes: (1)just to log the Client activity info
  *  for audit purposes in case of Login failure at the client side; (2) to verify
  *  the client identity and authorize it (if the verification is successful) and log this
  *  information for audit purposes in case Login at the client end is successful.
  *  </p>
  */
 public boolean verifyClient(AAISSession session) throws Event{

	 String principal = session.getAccessToken().getPrincipal();	 

      Hashtable NewAuditInfo = new Hashtable();
      NewAuditInfo.put("ACTION", "Login");

   int ClientSessionAcitivtyStatus = session.getActivityAuditStatus();
   if (ClientSessionAcitivtyStatus==0)
   {
      registerUserSessionActivity(NewAuditInfo, 0, session);
      return true;
   }

   if(AAISIdentity.isUserLocked("AAIS", m_applicationName, principal))
	{
		 throw new Event(null,  Event.ERROR, "Failed Login for Locked principal="+ principal, "", "Security" );
	}

   boolean RetStatus = verifyClient( session.getAccessToken() );

   session.setPrincipal(principal);

   if (RetStatus)
   {
      registerUserSessionActivity(NewAuditInfo, 1, session);
      return true;
   }
   else
   {
      registerUserSessionActivity(NewAuditInfo, 0, session);
      return false;
   }
 }

 public boolean checkFunctionAccess(String principal,String function) throws Event{
   return checkPrivilege( principal, function );
 }

 public boolean checkFunctionAccess(AAISSession session,String function) throws Event{
   return checkFunctionAccess( session.getPrincipal(), function );
 }



/**  <p> This method should be executed from the client side! If it been executed
  *  from the server side, then it will always return false! The reason being that
  *  from the server side the AAISApplication will be instantiated with the
  *  constructor which passes the servicename, and thus it will generate an
  *  accessToken with signature that of the servicename. The call
  *  <code> ec.runPrivCheck(m_accessToken.getSignature(),principal,rqTransaction)</code>
  *  requires that the signature passed is that of the principal, and thereefore
  *  if servicename != principal, the call will fail and return false.
  *  </p>
  */
 public boolean checkPrivilege(String principal, String privilegeBuffer) throws Event {

   if (principal.lastIndexOf(".")==-1) {
      principal = AAISApplicationName.concat(".").concat(principal);
   }
   String fEP = GFTTApplication.functionEntitlementPrefix; // = "Function."
   String appName = GFTTApplication.applicationName; // = "GFTT-TR"
   if (privilegeBuffer.startsWith(fEP) ) {
//      System.out.println("AAISApplication::checkPrivilege : "+
//      privilegeBuffer + " starts with "+fEP+" ; removing it.");
//      we need to do that so we can check the Fail_xxx part
      privilegeBuffer = privilegeBuffer.substring(fEP.length());
   }

   AAISUserApplicationProfile userApplicationProfile =
	                           new AAISUserApplicationProfile( this, principal );
   String privilegeName    = "Fail_"+ privilegeBuffer;
   AAISPrivilege privilege = userApplicationProfile.findPrivilege( privilegeName );
   if (privilege !=null) return false;
   // if privilege !=null, this means that there is a privilege named
   // "Fail_"+privilegeBuffer, and thus we return false; else, continue checking.

 String rqTransaction=null;
 try {
   Emo ec = (new EmoClient()).getEmo(appName);
   rqTransaction = new StringBuffer(fEP).append(privilegeBuffer).toString();
   boolean privcheck =  ec.runPrivCheck(m_applicationName,principal,rqTransaction);

  Event.trace("principal="+principal+" rqTransaction="+rqTransaction+
               " checkPrivilege="+privcheck, "runPrivCheck()", "PRIVILEGE" );


   return privcheck;

 } catch (Exception e) {
     Event.trace(e, Event.ERROR,
		   "Failure in the execution of runPrivCheck for principal="+principal+
                   " rqTransaction="+rqTransaction, "", "PRIVILEGE" );
     return false;
 }
 }


/**  <p> Same comments apply as above( <code>checkPrivilege(principal,privilegeBuffer)</code>).
  *  The principal of <code>session.getPrincipal()</code>
  *  has to be the same as the username that the AAISApplication was instantiated with.
  *  </p>
  */
 public boolean checkPrivilege(AAISSession session, String privilegeBuffer) throws Event{
   return checkPrivilege( session.getPrincipal(),privilegeBuffer);
 }


 public AAISArchive toArchive() throws Event{
  AAISArchive ar = new AAISArchive();
  ar.addElement(m_applicationName);
  ar.OutputClose();
  return ar;
 }

 public void fromArchive(AAISArchive arIn) throws Event{
   AAISArchive ar = null;
   ar = new AAISArchive(arIn.getBuffer(), arIn.getBufferSize() );
   m_applicationName = new String( ar.getElement() );

  ar.InputClose();

 }

 private void mf_init( String applicationName, AAISIdentity identity )
 throws Event {
try {
    Emo ec = (new EmoClient()).getEmo();
    // this function simply checks if the appName is in Indigo database
    if (! ec.runAppNameCheck(identity.getSignature(),applicationName) )
        throw new Event(null,  Event.ERROR,
		"Failure in the execution of runAppNameCheck for principal="+
                 identity.getPrincipal() , "", "EMO" );

    m_applicationName = applicationName;
    m_identity = identity;
    m_accessToken = new AAISAccessToken(applicationName, identity );

} catch (Exception e) {
    e.printStackTrace();
    throw new Event(e,  Event.ERROR, "failure in AAISApplication::mf_init", "", "EMO" );
} 

 }

  public String toString() {
   StringBuffer out = new StringBuffer();
   out.append("Application: " + m_applicationName + "\n" +
	(m_accessToken==null ? "accessToken=null" : m_accessToken.toString()) );

   return out.toString();
 }

 /** <p> This method is used to register the client activity information for audit purposes. The
  *  inputs : (1) a Hashtable that has one property named "ACTION" with the value as "Login" or
  *  "Logoff" or names representing other activities; (2) Status of the activity: 1 for success,
  *  0 for failure; (3) AAISSession object representing the client identity.
  *  </P>
  */
  public void registerUserSessionActivity(Hashtable AuditInfo, int Status, AAISSession Session) throws Event {
      Hashtable NewAuditInfo = new Hashtable();
      NewAuditInfo.put("LOGINID", Session.getPrincipal().toLowerCase());
      NewAuditInfo.put("APPLICATIONNAME", Session.getApplicationName());
      NewAuditInfo.put("ACTION", (String)AuditInfo.get("ACTION"));
	  
      Session.getAccessToken().registerUserSessionActivity(NewAuditInfo, Status);
 }


}
