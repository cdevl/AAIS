/*  AAISApplicationProfile.java
    Translated from C++: Panagiotis N. Zarros
    Wednsday Jan 10 2001
*/
package com.db.MT.Framework.AAISServiceCo;
import java.util.*;
import java.io.*;
//import com.db.MT.Framework.AAISRMIServerCo.Emo;
import com.db.MT.Framework.EventCo.*;

public class AAISApplicationProfile {

 protected LinkedList m_attributeList   = new LinkedList();
 protected LinkedList m_entitlementList = new LinkedList();
 protected String     m_applicationName;

 public AAISApplicationProfile() {}

 public AAISApplicationProfile(AAISAccessToken queryAccessToken,
                               String applicationName) throws Event {
	mf_init(queryAccessToken, applicationName);
 }

 public AAISApplicationProfile(AAISApplication application) throws Event {
     	mf_init(application.getAccessToken(),application.getApplicationName());
 }

 public String getApplicationName()     { return m_applicationName; }

 public LinkedList getAttributeList()   { return m_attributeList; }

 public LinkedList getEntitlementList() { return m_entitlementList; }

 public AAISAttribute findAttribute( String attributeName ) {
  if (m_attributeList==null  || attributeName==null) return null;
    ListIterator LI= m_attributeList.listIterator(0);
    while ( LI.hasNext() )  {
       AAISAttribute attribute = (AAISAttribute)LI.next();
       if ( attribute.getAttributeName().equalsIgnoreCase(attributeName) )
          return attribute;
    }
    return null;
 }

 public String findAttributeValue( String attributeName ) {
  if (attributeName==null || attributeName.trim().length() ==0) return null;
  AAISAttribute attribute = findAttribute(attributeName);
  if (attribute != null ) return attribute.getAttributeValue();
  return null;
 }



 public AAISEntitlement findEntitlement( String entitlementName ) {
   if (m_entitlementList==null || entitlementName==null ||
       entitlementName.trim().length() ==0)
      return null;
   ListIterator LI= m_entitlementList.listIterator(0);
   while ( LI.hasNext() )  {
     AAISEntitlement entitlement = (AAISEntitlement)LI.next();
     if ( entitlement.getEntitlementName().equalsIgnoreCase(entitlementName) )
        return entitlement;
   }
   return null;
 }

 public AAISAttribute findEntitlementAttribute( String entitlementName,
                                            	String attributeName ) {
  if (attributeName==null || entitlementName==null ||
       entitlementName.trim().length()==0 || attributeName.trim().length()==0 )
     return null;
  AAISEntitlement entitlement = findEntitlement(entitlementName);
  if (entitlement == null ) return null;

  AAISAttribute attribute = entitlement.findAttribute(attributeName);
  return attribute;
 }


 public String findEntitlementAttributeValue( String entitlementName,
                                              String attributeName ) {
   if (attributeName==null || entitlementName==null ||
       entitlementName.trim().length()==0 || attributeName.trim().length()==0 )
     return null;

   AAISAttribute attribute = findEntitlementAttribute(entitlementName,attributeName);
   if (attribute == null ) return null;
   return attribute.getAttributeValue();
 }


/** <p> Archives the AAISApplicationProfile into a hashtable of hashtable
  * of properties object. This Object has all the informnation pertaining <br>
  * to the Application Entitlement Attributes (all entitlements associated with this <br>
  * application plus all associated attributes with each Entitlement) <br>
  * </p>
   *  The structure of the hashtables are as follows:<br>
  *  key=applicationname                  Value=another hashtable aH<br>
  *  Key=applicationname+":Entitlements"  Value=another hashtable pH<br>
  *<br>
  *  The hashtable aH contains <b>one</b> key/value pair.<br>
  *  The key should most probably be the application name (i.e.<br>
  *  "AAIS.GFTT-TR"), but this should not be taken as granted and the value<br>
  *  is a Properties object which contains the descriptors.(it most<br>
  *  probably will have one key/value, <br>
  *  i.e. "description"/"Global Funds Transfer")<br>
  *<br>
  *  The hashtable pH contains <b>multiple</b> key/value pairs.<br>
  *  The keys correspond to the entitlements of this application,<br>
  *  which are all entitlements starting with "AppName." .<br>
  *  So, if there are 2 entries beginning with "GFTT-TR.", for example<br>
  *  GFTT-TR.Functions.TransactionInquiry and GFTT-TR.Dealer.Segment.Asia,<br>
  *  then the keys will be the strings "Functions.TransactionInquiry" and<br>
  *  "Dealer.Segment.Asia".<br>
  *  For each of those keys, the corresponding value is a Properties<br>
  *  object which contains the attributeNames/attributeValues pairs<br>
  *  <br>
  *  The Properties Object has the key/Value pairs of all<br>
  *  the atributes of the Entitlement.
  * </p>
  */
 public Hashtable toHash() {
    Properties p = new Properties();
    if (m_attributeList!=null) {
      ListIterator LI = m_attributeList.listIterator(0);
      while (LI.hasNext()) {
         AAISAttribute a = (AAISAttribute)LI.next();
         String a_name = a.getAttributeName();
         String a_value = a.getAttributeValue();
         if (a_name != null || a_name.trim().length() != 0)
            p.setProperty(a_name,(a_value==null ? "" : a_value) );
      } //End While
    }

    Hashtable aH = new Hashtable();
    aH.put(m_applicationName,p);

    Hashtable pH = new Hashtable();
    if (m_entitlementList!=null) {
      ListIterator LI = m_entitlementList.listIterator(0);
      while (LI.hasNext()) {
         AAISEntitlement e = (AAISEntitlement)LI.next();
         String   e_name   = e.getEntitlementName();
         Properties p2     = e.toArchive();
         if (p2!=null || e_name != null || e_name.trim().length() != 0)      pH.put(e_name,p2);
         else {
              Event.trace(" got null from entitlement.toArchive() for "+e_name,
                          "", "PRIVILEGE" );
         }
      } //End While
    } else {
        Event.trace(" m_entitlementList is null for appname="+m_applicationName, "",
                    "PRIVILEGE" );
    }

    Hashtable H = new Hashtable();
    String aais = AAISApplication.AAISApplicationName+"."; // = "AAIS."
    // emo returns AAIS.GFTT-TR for applicationName; to be compatible
    H.put(aais+m_applicationName,aH);
    H.put(aais+m_applicationName+":Entitlements",pH);

    return H;
 }

 public void fromArchive(Hashtable H) throws Exception {

   Hashtable aH = new Hashtable();
   Hashtable pH = new Hashtable();
   /* The following 2 lines would have worked if we had used "AAIS."+m_applicationName
     aH = (Hashtable) H.get(m_applicationName);
     pH = (Hashtable) H.get(m_applicationName+":Entitlements");
   */
   for (Enumeration e = H.keys(); e.hasMoreElements(); ) {
     String x = (String)e.nextElement();
     if ( x.endsWith(":Entitlements") )pH = (Hashtable) H.get(x);
     else  {
         aH = (Hashtable) H.get(x);
         // in case x is in the form xxx.xxx, i.e. AAIS.GFTT-TR,
         // make m_applicationName= "GFTT-TR", else if there is No ".",
         // i.e. x =  GFTT-TR, then take the whole x which is "GFTT-TR".
         // m_applicationName may not be have been set if we called the default constructor.
         if (m_applicationName==null) m_applicationName=
            x.substring( (x.lastIndexOf(".")==-1 ? 0 : x.lastIndexOf(".")+1)  );
     }

   } //End FOR

// there should only be one iteration for aH
   for (Enumeration e = aH.keys(); e.hasMoreElements(); ) {
      String appname = (String)e.nextElement();
      Properties p = (Properties)aH.get(appname);
      if ( p != null ) {
         for (Enumeration e2 = p.propertyNames(); e2.hasMoreElements(); ) {
           String propKey = (String)e2.nextElement();
           AAISAttribute attribute = new AAISAttribute(propKey,p.getProperty(propKey));
           m_attributeList.addLast(attribute);
         }
      }
   }

  for (Enumeration e = pH.keys(); e.hasMoreElements(); ) {
     AAISEntitlement entitlement = new AAISEntitlement();

     String entitle = (String)e.nextElement();
     if ( entitle == null || entitle.trim().length() == 0 ) continue;
     Properties p  =  (Properties)pH.get(entitle);
     // if there are no attributes for this entitlement, then p=null
     if ( p == null ) p  =  new Properties();
     p.setProperty("EntitlementName",entitle);
     entitlement.fromArchive(p);

     m_entitlementList.addLast(entitlement);
  }

 }

 public AAISArchive toArchive() throws Event{
   AAISArchive ar = new  AAISArchive();

    ar.addElement( (m_applicationName==null ? "" : m_applicationName) );

    if (m_attributeList==null)   ar.addElement(0);
    else {
       ar.addElement(m_attributeList.size());
       ListIterator LI =  m_attributeList.listIterator(0);
       while ( LI.hasNext() )  {
          AAISAttribute attribute = (AAISAttribute)LI.next();
          String attribName  = attribute.getAttributeName();
          String attribValue = attribute.getAttributeValue();
          ar.addElement( (attribName==null ? "" : attribName) );
          ar.addElement( (attribValue==null ? "" : attribValue) );
       }
    }

    if (m_entitlementList==null)   ar.addElement(0);
    else {
       ar.addElement(m_entitlementList.size());
       ListIterator LI =  m_entitlementList.listIterator(0);
       while ( LI.hasNext() )  {
          AAISEntitlement entitle = (AAISEntitlement)LI.next();
          String entitleName  = entitle.getEntitlementName();
          if ( entitleName == null || entitleName.trim().length() == 0) {
            ar.addElement(""); // we need to add an Element even of 0 size because
			       // we already indicated the size of m_entitlementList
            ar.addElement(0);  // we need to add a count of 0 to indicate 0 attributes
                               // for this entitlement
            continue; // skip the rest for this entitlement
          }
          ar.addElement(entitleName);
          LinkedList attribList = entitle.getAttributeList();

          // -------------attributes within entitlement
          if (attribList==null)   ar.addElement(0);
          else {
            ar.addElement(attribList.size());
            ListIterator LI2 =  attribList.listIterator(0);
            while ( LI2.hasNext() )  {
              AAISAttribute attrib = (AAISAttribute)LI2.next();
              String attribName  = attrib.getAttributeName();
              String attribValue = attrib.getAttributeValue();
              if ( attribName == null) { ar.addElement("");  ar.addElement(""); }
              else  {
                  ar.addElement(attribName);
                  if ( attribValue == null )  ar.addElement("");
                  else ar.addElement(attribValue);
              }
            }
          } //endIF
          // ------------ End attributes within entitlement
       } //endWhile
    } // endIF
  ar.OutputClose();
  return ar;
 }

/**  <p> Reconstruct the AAISApplicationProfile from AAISArchive <br>
  *  We do not go recursively down to AAISEntitlement -> AAISAttribute
  *  @author P. Zarros
  */
  public void fromArchive(AAISArchive arIn) throws Event{
    AAISArchive  ar = null;

      ar = new AAISArchive(arIn.getBuffer(), arIn.getBufferSize() );

      m_applicationName = new String( ar.getElementString() );
      for (int s = ar.getElementInt(); s > 0; --s ) {
        String attribName  = new String( ar.getElementString() );
        String attribValue = new String( ar.getElementString() );
        AAISAttribute attrib = new AAISAttribute(attribName,attribValue);
        m_attributeList.addLast(attrib);
      }

      // fill in the m_entitlementList
      for (int e_s = ar.getElementInt(); e_s > 0; --e_s ) {
        String entitleName  = new String( ar.getElementString() );
        AAISEntitlement entitle = new AAISEntitlement(entitleName);
        LinkedList attribList = new LinkedList();
        for (int s = ar.getElementInt(); s > 0 ; --s ) {
           String attribName  = new String( ar.getElementString() );
           String attribValue = new String( ar.getElementString() );
           entitle.addAttribute(attribName,attribValue);
        }

        m_entitlementList.addLast(entitle);
      }

  ar.InputClose();

  }



 private void mf_init(AAISAccessToken queryAccessToken,String applicationName) throws Event{

  ReadProp rP = new ReadProp();
  String behavior = rP.getBehavior();
  if (behavior.equals("local") ) {
	// to be implemented later
  } else if (behavior.equals("Indigo") ) {
     try {
     m_applicationName = applicationName;
     Emo ec = (new EmoClient()).getEmo();
     Hashtable H = ec.runAppProfileQuery(queryAccessToken.getSignature(), applicationName);

     fromArchive(H);
     //Event.trace(this.toString(), "runAppProfileQuery", "PRIVILEGE" );
     } catch (Exception e) {
        e.printStackTrace();
        throw new Event( e, Event.ERROR, "FAILURE_EXECUTING_MF_INIT_AAIS_APPLICATION_PROFILE");
     }
  } else {
     throw new Event(null,Event.ERROR, "local or Indigo expected in aais.properties",
                     "", "EMO" ); }

}


 public String toString() {
   StringBuffer out = new StringBuffer();
   out.append("ApplicationProfile: m_applicationName=" + m_applicationName+"<BR>\n");

   if (m_attributeList != null ) {
   ListIterator LI= m_attributeList.listIterator(0);
   while ( LI.hasNext() )
     out.append( ((AAISAttribute)LI.next()).toString()+"<BR>\n");
   }

   if (m_entitlementList != null ) {
   ListIterator LI2= m_entitlementList.listIterator(0);
   while ( LI2.hasNext() )
     out.append( ((AAISEntitlement)LI2.next()).toString()+"<BR>\n");
   }

   return out.toString();
 }




}
