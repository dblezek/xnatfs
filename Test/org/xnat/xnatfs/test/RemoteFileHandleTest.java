package org.xnat.xnatfs.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.HashSet;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.xnat.xnatfs.FileHandle;
import org.xnat.xnatfs.RemoteFileHandle;
import org.xnat.xnatfs.XNATConnection;
import org.xnat.xnatfs.xnatfs;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.auth.*;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.auth.BasicSchemeFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

import fuse.FuseOpen;
import fuse.FuseOpenSetter;

public class RemoteFileHandleTest {
  static Logger logger = Logger.getLogger ( RemoteFileHandleTest.class );
  static xnatfs x = new xnatfs ();

  @BeforeClass
  public static void configure () {
    BasicConfigurator.configure ();
    xnatfs.configureCache ();
    xnatfs.configureConnection ();
  }

  @Test
  public void testLargeFile () throws Exception {
    Logger.getLogger ( "org.xnat.xnatfs" ).setLevel ( Level.DEBUG );
    Logger.getLogger ( "org.apache" ).setLevel ( Level.WARN );
    Logger.getLogger ( "org.apache" ).setLevel ( Level.WARN );
    try {
      RemoteFileHandle files = new RemoteFileHandle ( "http://central.xnat.org/REST/projects/CENTRAL_OASIS_CS/subjects/OAS1_0456/experiments/OAS1_0456_MR1/scans/mpr-1/files", "files" );
      assertTrue ( files.getCachedFile () != null );
    } catch ( Exception e1 ) {
      fail ( "Failed to get remote file: " + e1 );
    }
  }

  @Test
  public void testJDOM () throws Exception {
    RemoteFileHandle fh = XNATConnection.getInstance ().get ( "http://central.xnat.org/REST/projects/NAMIC_TEST/subjects/1/experiments/MR1/scans/4/resources/SNAPSHOTS?format=json", "resources" );
    HashSet<String> list = new HashSet<String> ();
    fh.waitForDownload ();
    InputStreamReader reader = new InputStreamReader ( new FileInputStream ( fh.getCachedFile () ) );
    SAXBuilder builder = new SAXBuilder ();
    Document doc = builder.build ( reader );
    org.jdom.Element root = doc.getRootElement ();
  }

  @Test
  public void testFileHandle () throws Exception {
    FileHandle fh = null;
    try {
      fh = new FileHandle ( "http://central.xnat.org/REST/projects/CENTRAL_OASIS_CS/subjects?format=xml", "foo" );
      fh.open ();
      assertTrue ( fh.waitForDownload () != 0 );
    } catch ( Exception e2 ) {
      logger.error ( "Error", e2 );
      fail ( "Failed to get remote file through FileHandle: " + e2 );
    } finally {
      fh.release ();
    }
  }

  @Test
  public void readFileHandle () throws Exception {
    FileHandle fh = null;
    try {
      fh = new FileHandle ( "http://central.xnat.org/REST/projects/CENTRAL_OASIS_CS/subjects?format=json", "foobar" );
      fh.open ();
      ByteBuffer b = ByteBuffer.allocate ( 1000000 );
      int offset = 0;
      while ( !fh.isDownloadComplete () ) {
        fh.read ( b, offset );
        offset += b.position ();
        logger.debug ( "Buffer position: " + b.position () );
      }
    } catch ( Exception e2 ) {
      logger.error ( "Error", e2 );
      fail ( "Failed to get remote file through FileHandle: " + e2 );
    } finally {
      fh.release ();
    }

    // try {
    // RemoteFileHandle bigFile = new RemoteFileHandle (
    // "http://central.xnat.org/REST/projects/CENTRAL_OASIS_CS/subjects/OAS1_0456/experiments/OAS1_0456_MR1/scans/mpr-1/resources/308/files/OAS1_0456_MR1_mpr-1_anon.img",
    // "bigfile" );
    // assertTrue ( bigFile.getCachedFile () != null );
    // } catch ( Exception e2 ) {
    // fail ( "Failed to get large remote file: " + e2 );
    // }

  }

  @Test
  public void testCommons () throws Exception {
    DefaultHttpClient httpclient = new DefaultHttpClient ();

    httpclient.getCredentialsProvider ().setCredentials ( new AuthScope ( AuthScope.ANY_HOST, AuthScope.ANY_PORT ), new UsernamePasswordCredentials ( "blezek", "throwaway " ) ); // .setCredentials
    // (
    // new
    // AuthScope
    // (
    // "localhost",
    // 443
    // ),
    // new
    // UsernamePasswordCredentials
    // (
    // "username",
    // "password"
    // )
    // );

    // httpclient.getAuthSchemes ().register ( "basic", new BasicSchemeFactory
    // () );

    logger.debug ( httpclient.getAuthSchemes ().getSchemeNames () );

    logger.debug ( httpclient.getTargetAuthenticationHandler () );
    HttpGet httpget = new HttpGet ( "http://central.xnat.org/REST/projects/CENTRAL_OASIS_CS/subjects/OAS1_0456/experiments/OAS1_0456_MR1/scans/mpr-1/files" );

    // BasicHttpContext localcontext = new BasicHttpContext ();

    // Generate BASIC scheme object and stick it to the local
    // execution context
    // BasicScheme basicAuth = new BasicScheme ();
    // localcontext.setAttribute ( "preemptive-auth", basicAuth );

    // Add as the first request interceptor
    // httpclient.addRequestInterceptor ( new PreemptiveAuth (), 0 );

    System.out.println ( "executing request " + httpget.getRequestLine () );
    HttpResponse response = httpclient.execute ( httpget );
    HttpEntity entity = response.getEntity ();

    System.out.println ( "----------------------------------------" );
    System.out.println ( response.getStatusLine () );
    if ( entity != null ) {
      System.out.println ( "Response content length: " + entity.getContentLength () );
    }
    if ( entity != null ) {
      entity.consumeContent ();
    }

    // When HttpClient instance is no longer needed,
    // shut down the connection manager to ensure
    // immediate deallocation of all system resources
    httpclient.getConnectionManager ().shutdown ();

  }

  @Test
  public void Interceptor () throws Exception {
    DefaultHttpClient httpclient = new DefaultHttpClient ();

    httpclient.getCredentialsProvider ().setCredentials ( new AuthScope ( AuthScope.ANY_HOST, AuthScope.ANY_PORT ), new UsernamePasswordCredentials ( "blezek", "throwaway" ) );

    BasicHttpContext localcontext = new BasicHttpContext ();

    // Generate BASIC scheme object and stick it to the local
    // execution context
    BasicScheme basicAuth = new BasicScheme ();
    localcontext.setAttribute ( "preemptive-auth", basicAuth );

    // Add as the first request interceptor
    httpclient.addRequestInterceptor ( new PreemptiveAuth (), 0 );

    HttpHost targetHost = new HttpHost ( "central.xnat.org", 80, "http" );

    HttpGet httpget = new HttpGet ( "/REST/projects/CENTRAL_OASIS_CS/subjects/OAS1_0456/experiments/OAS1_0456_MR1/scans/mpr-1/files" );

    logger.debug ( "executing request: " + httpget.getRequestLine () );
    logger.debug ( "to target: " + targetHost );

    for ( int i = 0; i < 3; i++ ) {
      logger.debug ( "Starting loop: " + i );
      HttpResponse response = httpclient.execute ( targetHost, httpget, localcontext );
      logger.debug ( "Got response: " + response );
      HttpEntity entity = response.getEntity ();

      System.out.println ( "----------------------------------------" );
      System.out.println ( response.getStatusLine () );
      if ( entity != null ) {
        System.out.println ( "Response content length: " + entity.getContentLength () );
        entity.consumeContent ();
      }
    }
    // When HttpClient instance is no longer needed,
    // shut down the connection manager to ensure
    // immediate deallocation of all system resources
    httpclient.getConnectionManager ().shutdown ();

  }

  @Ignore
  public void testOpen () throws Exception {
    String path = "/projects/CENTRAL_OASIS_CS/subjects/OAS1_0457/experiments/OAS1_0457_MR1/scans/mpr-1/files/OAS1_0457_MR1_mpr-1_anon.img";
    FuseOpenSetter openSetter = new FuseOpen ();
    x.open ( path, 0, openSetter );
  }

  static class PreemptiveAuth implements HttpRequestInterceptor {

    public void process ( final HttpRequest request, final HttpContext context ) throws HttpException, IOException {

      AuthState authState = (AuthState) context.getAttribute ( ClientContext.TARGET_AUTH_STATE );

      // If no auth scheme avaialble yet, try to initialize it preemptively
      if ( authState.getAuthScheme () == null ) {
        AuthScheme authScheme = (AuthScheme) context.getAttribute ( "preemptive-auth" );
        CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute ( ClientContext.CREDS_PROVIDER );
        HttpHost targetHost = (HttpHost) context.getAttribute ( ExecutionContext.HTTP_TARGET_HOST );
        if ( authScheme != null ) {
          Credentials creds = credsProvider.getCredentials ( new AuthScope ( targetHost.getHostName (), targetHost.getPort () ) );
          if ( creds == null ) {
            throw new HttpException ( "No credentials for preemptive authentication" );
          }
          authState.setAuthScheme ( authScheme );
          authState.setCredentials ( creds );
        }
      }

    }
  }

}
