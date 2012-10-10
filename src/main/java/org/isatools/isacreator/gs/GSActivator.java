package org.isatools.isacreator.gs;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import org.isatools.isacreator.api.Authentication;
import org.isatools.isacreator.gui.ISAcreator;
import org.isatools.isacreator.gui.modeselection.Mode;
import org.isatools.isacreator.launch.ISAcreatorCLArgs;


/**
 * Created by the ISATeam.
 * User: agbeltran
 * Date: 08/10/2012
 * Time: 23:10
 *
 * @author <a href="mailto:alejandra.gonzalez.beltran@gmail.com">Alejandra Gonzalez-Beltran</a>
 */
public class GSActivator implements BundleActivator {

    private ISAcreator main = null;

    public void start(final BundleContext bundleContext) throws Exception {

        Thread loadISATask = new Thread(new Runnable() {
            public void run() {

                //this shouldn't happen
                if (ISAcreatorCLArgs.mode()!= Mode.GS){
                    System.exit(-1);
                }

                Authentication gsAuthentication = null;

                //LOGIN

                //main = new ISAcreator(ISAcreatorCLArgs.mode(), bundleContext, ISAcreatorCLArgs.configDir());
                main = new ISAcreator(ISAcreatorCLArgs.mode(), bundleContext);

                //if username and password were given as parameters, log in user into GS
                if (ISAcreatorCLArgs.username()!=null && ISAcreatorCLArgs.password()!=null){

                     gsAuthentication = new GSIdentityManager();
                     boolean loggedIn = gsAuthentication.login(ISAcreatorCLArgs.username(), ISAcreatorCLArgs.password().toCharArray());
                     if (!loggedIn){
                        System.out.println("Login to GenomeSpace failed for user "+ISAcreatorCLArgs.username());
                        System.exit(0);
                     }


                     main.createGUI(ISAcreatorCLArgs.configDir(), ISAcreatorCLArgs.username(), ISAcreatorCLArgs.isatabDir());

                } else if (ISAcreatorCLArgs.username()!=null){
                    //if username identified, check if token exists
                    gsAuthentication =  new GSSingleSignOnManager();
                    boolean loggedIn = gsAuthentication.login(ISAcreatorCLArgs.username());


                }else {
                      //both username and password are null
                      gsAuthentication =  new GSSingleSignOnManager();
                      main.createGUI(ISAcreatorCLArgs.configDir(), ISAcreatorCLArgs.username(), ISAcreatorCLArgs.isatabDir(), gsAuthentication, "org.isatools.isacreator.gs.gui.GSAuthenticationMenu");

                }//else
            }//run

        });

        loadISATask.start();
    }

    public void stop(BundleContext bundleContext) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
