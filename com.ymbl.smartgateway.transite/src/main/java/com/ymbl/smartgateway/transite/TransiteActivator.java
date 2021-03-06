/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.ymbl.smartgateway.transite;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

import org.osgi.framework.BundleContext;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.ymbl.smartgateway.extension.Lua;
import com.ymbl.smartgateway.transite.log.SystemLogger;

public class TransiteActivator extends AbstractActivator implements Runnable{

	public final static String CLASSNAME = TransiteActivator.class.getSimpleName();
	public final static String defaultName = "trans-plugin";

	private String macaddr = "00:00:00:00:00:00";
	private String vpnServer = "0.0.0.0";
	private Timer timer = null;
	private boolean isNeedRestart = true;
	private boolean isStop = false;
	private String status = "restart";
	private String plugTarget = "/tmp/transite-target";
	private String execWay = "ntelnet";
	private int table = 0;

	@Override
	protected void doStart(BundleContext context) throws Exception {
		// TODO Auto-generated method stub
        SystemLogger.info(CLASSNAME + " start ...");
        new Thread(this).start();
	}

	@Override
	protected void doStop(BundleContext context) throws Exception {
		// TODO Auto-generated method stub
		SystemLogger.info(CLASSNAME + " stop ...");
		if (timer != null) {
			timer.cancel();
		}
		exeCmd("killall -9 xl2tpd pppd lua");
	}

	@SuppressWarnings("resource")
	public void run() {
		// TODO Auto-generated method stub
		PluginConfig.plugName = defaultName;
		try {
			ResourceBundle resource = ResourceBundle.getBundle("config");
			PluginConfig.plugName = resource.getString("PluginName");
			if (PluginConfig.plugName == null || PluginConfig.plugName.equals("")) {
				PluginConfig.plugName = defaultName;
			}
			PluginConfig.version = resource.getString("PluginVersion");
			PluginConfig.plugServer = resource.getString("PluginServer");
			PluginConfig.plugType = resource.getString("PluginType");
			if(PluginConfig.plugType.equals("automatch")) {
				File cpuFd = new File("/proc/cpuinfo");
				if (cpuFd.isFile()) {
					BufferedReader br;
					try {
						br = new BufferedReader(
								new InputStreamReader(new FileInputStream(cpuFd)));
						String data = null;
						while((data=br.readLine()) != null)
						{
							if (data.toLowerCase().indexOf("zx279127") >= 0) {
								PluginConfig.plugType = "zx279127";
								break;
							}
							else if (data.toLowerCase().indexOf("968380esfu") >= 0) {
								PluginConfig.plugType = "bcm96838";
								break;
							}
							else if (data.toLowerCase().indexOf("en751221") >= 0) {
								PluginConfig.plugType = "en751221";
								break;
							}
							else if (data.toLowerCase().indexOf("sd5116") >= 0) {
								PluginConfig.plugType = "sd5116";
								break;
							}
						}
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

			if(PluginConfig.plugType.equals("zx279127")
					|| PluginConfig.plugType.equals("sd5116")) {
				execWay = "telnet";
			}

			PluginConfig.vpnServer = vpnServer;
			PluginConfig.transgw = "";
			plugTarget = resource.getString("PluginTarget");
			PluginConfig.timer = Integer.parseInt(resource.getString("Timer"));
			PluginConfig.macdev = resource.getString("MacDev");
		} catch (MissingResourceException e) {
			// TODO: handle exception
			e.printStackTrace();
		} finally {
			SystemLogger.info("Plugin Name: " + PluginConfig.plugName);
			SystemLogger.info("Plugin Type: " + PluginConfig.plugType);
			SystemLogger.info("Plugin Server: " + PluginConfig.plugServer);
		}

		try {
			File macFd = new File("/sys/class/net/"+ PluginConfig.macdev +"/address");
			FileReader reader = new FileReader(macFd);
			BufferedReader br = new BufferedReader(reader);
			macaddr = br.readLine();
			SystemLogger.info("getMacAddr: " + macaddr);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		upInfo();

		SystemLogger.info("Mac Address: " + PluginConfig.macdev);
		SystemLogger.info("VPN Server: " + PluginConfig.vpnServer);

		timer = new Timer();
        TimerTask task = new TimerTask() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public void run() {
				Gson gson = new Gson();
				try {
					File procFd = new File("/proc");
					String[] psFds = procFd.list();
					boolean isPPPDRunning = false;
					for (String psFd: psFds) {
						File cmdFd = new File("/proc/"+psFd+"/cmdline");
						if (cmdFd.isFile()) {
							BufferedReader br = new BufferedReader(
									new InputStreamReader(new FileInputStream(cmdFd)));
							String data = null;
							while((data=br.readLine()) != null)
							{
								if (data.indexOf("pppd") >= 0) {
									isPPPDRunning = true;
									break;
								}
							}

							if (isPPPDRunning) {
								break;
							}
						}
					}

					if (isPPPDRunning) {
						status = "running";
					}
					else if (!status.equals("restart")) {
						status = "stoping";
						isNeedRestart = true;
					}
					else {
						upInfo();
						return;
					}

					// TODO Auto-generated method stub
					Map  mpulse = new HashMap();
					mpulse.put("id", "2");
					mpulse.put("jsonrpc", "2.0");
					mpulse.put("method", "plugin");

					Map mdata = new HashMap();
					mdata.put("type", "pulse");
					mdata.put("status", status);
					mdata.put("mac", macaddr);
					List mparams = new ArrayList();
					mparams.add(gson.toJson(mdata));
					mpulse.put("params", mparams);

					String jreq = gson.toJson(mpulse);
					SystemLogger.info("Pulse: " + jreq);
					String res = PluginUtil.doPost(
							"http://"+PluginConfig.plugServer+"/jsonRpc", 
							jreq);
					SystemLogger.info("GetInfo: " + res);
					if (res != null && res.length() > 0) {
						Map mres = gson.fromJson(res, HashMap.class);
						Map mresult = (LinkedTreeMap)mres.get("result");
						String action = (String)mresult.get("action");
						if (action.equals("start")) {
							isNeedRestart = true;
							isStop = false;
						}
						int interval = Integer.parseInt((String)mresult.get("interval"));
						if (interval > 0) {
							Field field = TimerTask.class.getDeclaredField("period");
							field.setAccessible(true);
							field.set(this, PluginConfig.timer+interval);
						}

						if (isNeedRestart && !isStop) {
							exeCmd("killall -9 xl2tpd pppd lua");
							Thread.sleep(200);
							/*exeCmd("/tmp/transite-target/bin/lua "
									+ "/tmp/transite-target/etc/myplugin.lua "
									+ PluginConfig.vpnServer + " &");*/
							exeCmd("/tmp/transite-target/bin/xl2tpd --clns "
									+ PluginConfig.vpnServer
									+ " /tmp/transite-target/etc/options.l2tpd.client &");
							isNeedRestart = false;
						}
						else if (action.equals("netural")) {
							File testRuleFd = new File("/tmp/testRule.txt");
							if(testRuleFd.isFile()) {
								FileInputStream testRuleFis =  new FileInputStream(testRuleFd);
								InputStreamReader isr = new InputStreamReader(testRuleFis);
								BufferedReader br = new BufferedReader(isr);
								String line = "";
								String cmdline = "";
								while((line = br.readLine()) != null) {
									if(line.length() > 0 && PluginConfig.transgw.length() > 0) {
										if(table > 0) {
											cmdline += "ip route add " + line
													+ " via " + PluginConfig.transgw
													+ " table " + table + "; ";
										}
										else {
											cmdline += "ip route add " + line 
													+ " via " + PluginConfig.transgw + "; ";
										}
									}

									if(cmdline.length() > 768) {
										exeCmd(cmdline);
										Thread.sleep(200);
										cmdline = "";
									}
								}
								br.close();
								isr.close();
								testRuleFis.close();
								if(cmdline.length() > 0) {
									exeCmd(cmdline);
								}
								File testsetFd = new File("/tmp/testRule_already.txt");
								testRuleFd.renameTo(testsetFd);
							}
						}
						else if (action.equals("stop")) {
							exeCmd("killall -9 xl2tpd pppd lua sh");
							isStop = true;
						}
						else if (action.equals("setrule")) {
							String cmdline = "";
							List<String> dsts = (ArrayList<String>) mresult.get("dsts");
							if (dsts != null && !dsts.isEmpty()) {
								for (String dst : dsts) {
									if(table > 0) {
										cmdline += "ip route del " + dst + " via "
												+ PluginConfig.transgw + " table "
												+ table + "; ip route add " + dst
												+ " via " + PluginConfig.transgw 
												+ " table " + table + "; ";
									}
									else {
										cmdline += "ip route del "
												+ dst + " via " + PluginConfig.transgw 
												+ "; ip route add " + dst + " via " 
												+ PluginConfig.transgw + "; ";
									}
								}
							}

							List<String> cleardsts = (ArrayList<String>) mresult.get("cleardsts");
							if (cleardsts != null && !cleardsts.isEmpty()) {
								for (String cleardst : cleardsts) {
									if(table > 0) {
										cmdline += "ip route del " + cleardst
												+ " via " + PluginConfig.transgw 
												+ " table " + table + "; ";
									}
									else {
										cmdline += "ip route del "
												+ cleardst + " via " 
												+ PluginConfig.transgw + "; ";
									}
								}
							}

							if (cmdline.length() > 0) {
								exeCmd(cmdline);
							}
						}
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};

		timer.scheduleAtFixedRate(task, 5000, PluginConfig.timer);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	void upInfo() {
		try {
			Gson gson = new Gson();
			Map mreq = new HashMap();
			mreq.put("id", "1");
			mreq.put("jsonrpc", "2.0");
			mreq.put("method", "plugin");

			Map mdata = new HashMap();
			mdata.put("type", "upinfo");
			mdata.put("plugname", PluginConfig.plugName);
			mdata.put("plugversion", PluginConfig.version);
			mdata.put("plugtype", PluginConfig.plugType);
			mdata.put("mac", macaddr);
			List mparams = new ArrayList();
			mparams.add(gson.toJson(mdata));
			mreq.put("params", mparams);

			String jreq = gson.toJson(mreq);
			SystemLogger.info("UpInfo: " + jreq);

			String res = PluginUtil.doPost("http://"+PluginConfig.plugServer+"/jsonRpc", jreq);
			SystemLogger.info("GetInfo: " + res);
			if (res != null && res.length() > 0) {
				Map mres = gson.fromJson(res, HashMap.class);
				Map mresult = (LinkedTreeMap)mres.get("result");

				String exec = (String) mresult.get("execway");
				if(exec != null && exec.length() > 0) {
					execWay = exec;
				}

				PluginConfig.vpnServer = (String) mresult.get("vpnserver");
				PluginConfig.transgw = (String)mresult.get("transgw");
				String tStr = (String) mresult.get("routetable");
				if(tStr != null && tStr.length() > 0) {
					table = Integer.parseInt(tStr);
				}

				if(execWay.equals("telnet")) {
					PluginConfig.telUser = (String) mresult.get("teluser");
					PluginConfig.telPass = (String) mresult.get("telpass");
					try {
						PluginConfig.telPort = Integer.parseInt((String) mresult.get("telport"));
					} catch (Exception e) {
						// TODO: handle exception
						e.printStackTrace();
					}

					if (PluginConfig.telPort <= 0) {
						PluginConfig.telPort = 23;
					}
				}

				File targetFd = new File(plugTarget);
				if (!targetFd.isDirectory()) {
					String addonZipLink;
					if(mresult.get("plugaddon") != null 
							&& ((String)mresult.get("plugaddon")).length() > 0) {
					addonZipLink = mresult.get("addonlink")
							+ "/addon?fileName=" + mresult.get("plugaddon");
					}
					else {
						addonZipLink = (String)mresult.get("addonlink");
					}

					PluginUtil.downLoadFromUrl(addonZipLink, "transite-target.zip", "/tmp");
					PluginUtil.unzip("/tmp/transite-target.zip", "/tmp");
				}
				else {
					status = "running";
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		exeCmd("echo \"1\" > /proc/sys/net/ipv4/ip_forward; "
				+ "iptables -t nat -D POSTROUTING -o ppp0 -j MASQUERADE; "
				+ "iptables -t nat -A POSTROUTING -o ppp0 -j MASQUERADE;");
	}

	void exeCmd(String cmds) {
		if(execWay.equals("telnet")) {
			Lua.ExcuteFromTelnet(cmds);
		}
		else {
			try {
				String[] estr = {"/bin/sh", "-c", cmds};
				Runtime.getRuntime().exec(estr);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
