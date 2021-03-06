package org.xnat.xnatfs;

import fuse.*;

import java.util.*;
import org.apache.log4j.*;

import net.sf.ehcache.*;

/**
 * Class to handle a users. Shows up as a directory with three files in it.
 */
public class Subjects extends Container {

  static final Logger logger = Logger.getLogger ( Subjects.class );

  public Subjects ( String path ) {
    super ( path );
    mChildKey = "subjectid";
  }

  @Override
  public int getattr ( String path, FuseGetattrSetter setter ) throws FuseException {
    logger.debug ( "getattr: " + path );
    if ( path.equals ( mPath ) ) {
      setter.set ( this.hashCode (), FuseFtypeConstants.TYPE_DIR | 0755, 0, 0, 0, 0, 1, 1, xnatfs.sTimeStamp, xnatfs.sTimeStamp, xnatfs.sTimeStamp );
      return 0;
    }
    return Errno.ENOENT;
  }

  @Override
  public int getdir ( String path, FuseDirFiller filler ) throws FuseException {
    logger.debug ( "getdir: " + path );
    if ( path.equals ( mPath ) ) {
      return super.getdir ( path, filler, "label" );
    }
    return Errno.ENOTDIR;
  }

  /**
   * Create a child of this node. Note, the child is a single filename, not a
   * path
   */
  @Override
  public Node createChild ( String child ) throws FuseException {
    String childPath = mPath + "/" + child;
    logger.debug ( "Create child: " + child + " w/path: " + childPath );
    HashSet<String> subjectList = getElementList ( "label" );
    if ( subjectList.contains ( child ) ) {
      if ( xnatfs.sNodeCache.get ( childPath ) != null ) {
        return (Node) ( xnatfs.sNodeCache.get ( childPath ).getObjectValue () );
      }
      Element element = new Element ( childPath, new Subject ( childPath, child ) );
      xnatfs.sNodeCache.put ( element );
      return (Node) element.getObjectValue ();
    }
    return super.createChild ( child );
  }

}
