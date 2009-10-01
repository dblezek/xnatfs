/**
 * 
 */
package org.xnat.xnatfs.webdav;

import java.util.HashSet;

import org.apache.log4j.Logger;

import com.bradmcevoy.http.Resource;

/**
 * @author blezek
 * 
 */
public class Experiment extends VirtualDirectory {
  final static Logger logger = Logger.getLogger ( Experiment.class );

  /**
   * @param x
   * @param path
   * @param name
   * @param url
   */
  public Experiment ( XNATFS x, String path, String name, String url ) {
    super ( x, path, name, url );
    mElementURL = mURL + "scans?format=json";
    mChildKey = "id";
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xnat.xnatfs.webdav.VirtualDirectory#child(java.lang.String)
   */
  @Override
  public Resource child ( String childName ) {
    logger.debug ( "child: create " + childName );
    HashSet<String> s = null;
    try {
      s = getElementList ( mElementURL, mChildKey );
    } catch ( Exception e ) {
      logger.error ( "Failed to get child element list: " + e );
    }
    if ( true || s.contains ( childName ) ) {
      Scan scan = new Scan ( xnatfs, mAbsolutePath, childName, mURL + "scans/" + childName + "/" );
      setChildAuthorization ( scan );
      return scan;
    }
    return null;
  }
}
