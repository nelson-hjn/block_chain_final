package org.fisco.bcos.asset.client;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.awt.Toolkit;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
///////////////////////////////////////////////////////////////


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.fisco.bcos.asset.contract.Asset;
import org.fisco.bcos.asset.contract.Asset.RegisterEventEventResponse;
import org.fisco.bcos.asset.contract.Asset.TransferEventEventResponse;
import org.fisco.bcos.channel.client.Service;
import org.fisco.bcos.web3j.crypto.Credentials;
import org.fisco.bcos.web3j.crypto.Keys;
import org.fisco.bcos.web3j.protocol.Web3j;
import org.fisco.bcos.web3j.protocol.channel.ChannelEthereumService;
import org.fisco.bcos.web3j.protocol.core.methods.response.TransactionReceipt;
import org.fisco.bcos.web3j.tuples.generated.Tuple3;
import org.fisco.bcos.web3j.tuples.generated.Tuple2;
import org.fisco.bcos.web3j.tuples.generated.Tuple1;
import org.fisco.bcos.web3j.tx.gas.StaticGasProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class AssetClient {

	private JFrame frm = new JFrame();;
    
    private JTextField asset_register_account = new JTextField("用户名",30) ;
    private JTextField asset_register_amount = new JTextField("用户名",30) ;
    private JTextField asset_transfer_from_account  = new JTextField("用户名from",30) ;
    private JTextField asset_transfer_to_account  = new JTextField("用户名to",30) ;
    private JTextField asset_transfer_amount   = new JTextField("数额",30) ;
    private JTextField asset_select_account = new JTextField("用户名",30) ;
    private JTextField asset_select_amount  = new JTextField("数额",30) ;
    private JTextField asset_add_account = new JTextField("用户名",30) ;
    private JTextField asset_add_amount   = new JTextField("数额",30) ;
    private JTextField asset_remove_account_account  = new JTextField("用户名",30) ;
    private JTextField asset_remove_account_amount   = new JTextField("结果",30) ;
    

    /*private JTextField ;
    private JTextField ;
    private JTextField ;
    private JTextField ;
    private JTextField ;
    private JTextField ;*/


    /*private JButton ;
    private JButton ;*/


	static Logger logger = LoggerFactory.getLogger(AssetClient.class);

	private Web3j web3j;

	private Credentials credentials;

	public Web3j getWeb3j() {
		return web3j;
	}

	public void setWeb3j(Web3j web3j) {
		this.web3j = web3j;
	}

	public Credentials getCredentials() {
		return credentials;
	}

	public void setCredentials(Credentials credentials) {
		this.credentials = credentials;
	}

	public void recordAssetAddr(String address) throws FileNotFoundException, IOException {
		Properties prop = new Properties();
		prop.setProperty("address", address);
		final Resource contractResource = new ClassPathResource("contract.properties");
		FileOutputStream fileOutputStream = new FileOutputStream(contractResource.getFile());
		prop.store(fileOutputStream, "contract address");
	}

	public String loadAssetAddr() throws Exception {
		// load Asset contact address from contract.properties
		Properties prop = new Properties();
		final Resource contractResource = new ClassPathResource("contract.properties");
		prop.load(contractResource.getInputStream());

		String contractAddress = prop.getProperty("address");
		if (contractAddress == null || contractAddress.trim().equals("")) {
			throw new Exception(" load Asset contract address failed, please deploy it first. ");
		}
		logger.info(" load Asset address from contract.properties, address is {}", contractAddress);
		return contractAddress;
	}

	public void initialize() throws Exception {

		// init the Service
		@SuppressWarnings("resource")
		ApplicationContext context = new ClassPathXmlApplicationContext("classpath:applicationContext.xml");
		Service service = context.getBean(Service.class);
		service.run();

		ChannelEthereumService channelEthereumService = new ChannelEthereumService();
		channelEthereumService.setChannelService(service);
		Web3j web3j = Web3j.build(channelEthereumService, 1);

		// init Credentials
		Credentials credentials = Credentials.create(Keys.createEcKeyPair());

		setCredentials(credentials);
		setWeb3j(web3j);

		logger.debug(" web3j is " + web3j + " ,credentials is " + credentials);
	}

	private static BigInteger gasPrice = new BigInteger("30000000");
	private static BigInteger gasLimit = new BigInteger("30000000");

	public void deployAssetAndRecordAddr() {

		try {
			Asset asset = Asset.deploy(web3j, credentials, new StaticGasProvider(gasPrice, gasLimit)).send();
			System.out.println(" deploy Asset success, contract address is " + asset.getContractAddress());

			recordAssetAddr(asset.getContractAddress());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			System.out.println(" deploy Asset contract failed, error message is  " + e.getMessage());
		}
	}



	public BigInteger addAsset(String assetAccount, BigInteger addAccount) {
		
		try {
			String contractAddress = loadAssetAddr();
  
			Asset asset = Asset.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
			TransactionReceipt receipt = asset.assetAdd(assetAccount, addAccount).send();
			Tuple2<BigInteger,BigInteger>  response = asset.getAssetAddOutput(receipt);
			
				if ( response.getValue1().compareTo(new BigInteger("1")) == 0) {
					System.out.printf(" add asset account success => asset: %s, addvalue: %s \n", assetAccount,
							addAccount);
					return new BigInteger("1");
				  
				} else {
					System.out.printf(" add asset failed");
				}
			
		} catch (Exception e) {
			logger.error(" addAsset exception, error message is {}", e.getMessage());

			System.out.printf(" add asset account failed, error message is %s\n", e.getMessage());
		}
		return new BigInteger("0");
		
	}

	public BigInteger pillCheck(String account) {
		try {
			String contractAddress = loadAssetAddr();
  
			Asset asset = Asset.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
			TransactionReceipt receipt = asset.check(account).send();
			Tuple2<BigInteger, BigInteger> response = asset.getCheckOutput(receipt);

			if (response.getValue1().compareTo(new BigInteger("0")) == 0) {
				System.out.printf(" check account failed");
			}else {
				System.out.printf("check account succeed %s \n",response.getValue1());
				return response.getValue1();
			}


		} catch (Exception e) {
			logger.error(" check account exception, error message is {}", e.getMessage());

			System.out.printf(" check account  failed, error message is %s\n", e.getMessage());

		}
		return new BigInteger("0");
	}

	public BigInteger pillTransferPill(String payToNew, BigInteger pillId) {
		try {
			String contractAddress = loadAssetAddr();

			Asset asset = Asset.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
			TransactionReceipt receipt = asset.changepill(payToNew,pillId).send();
			Tuple2<BigInteger, BigInteger> response = asset.getChangepillOutput(receipt);

			if (response.getValue1().compareTo(new BigInteger("1")) == 0) {
				System.out.printf("transfer pill succeed \n");
				return new BigInteger("1");
			}else {
				System.out.printf(" transfer pill failed");
			}

		} catch (Exception e) {
			logger.error(" transfer pill id exception, error message is {}", e.getMessage());

			System.out.printf(" transfer pill  failed, error message is %s\n", e.getMessage());

		}
		return new BigInteger("0");
	}

	public BigInteger pillSelectForId(String payFrom, String payTo, BigInteger payAccount) {
		try {
			String contractAddress = loadAssetAddr();
  
			Asset asset = Asset.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
			TransactionReceipt receipt = asset.selectForId(payFrom, payTo, payAccount).send();
			Tuple2<BigInteger, BigInteger> response = asset.getSelectForIdOutput(receipt);

			if (response.getValue1().compareTo(new BigInteger("0")) == 0) {
				System.out.printf(" select pill id failed \n");
			}else {
				System.out.printf("select pill id succeed  %s \n",response.getValue1());
				return response.getValue1();
			}

		} catch (Exception e) {
			logger.error(" select pill id exception, error message is {}", e.getMessage());

			System.out.printf(" select pill id failed, error message is %s\n", e.getMessage());

		}
		return new BigInteger("0");
	}

	public BigInteger pillDeletePill(String payFrom, String payTo, BigInteger payAccount, BigInteger pillId) {
        String payFrom2 = payFrom;
        String payTo2 = payTo;
        if (payTo == "") {
        	payTo2 = "empty";
        }
        if (payFrom == "") {
        	payFrom2 = "empty";
        }

		try {
			String contractAddress = loadAssetAddr();
  
			Asset asset = Asset.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
			TransactionReceipt receipt = asset.deletepill(payFrom2, payTo2, payAccount, pillId).send();
			Tuple2<BigInteger, BigInteger> response = asset.getDeletepillOutput(receipt);

			if (response.getValue1().compareTo(new BigInteger("1")) == 0) {
				System.out.printf("delete pill succeed \n");
				return new BigInteger("1");
			}else {
				System.out.printf(" delete pill failed");
			}

		} catch (Exception e) {
			logger.error(" delete pill exception, error message is {}", e.getMessage());

			System.out.printf(" delete pill account failed, error message is %s\n", e.getMessage());


		}
		return new BigInteger("0");
	}

	public Tuple3<List<byte[]>, List<byte[]>, List<BigInteger>> pillSelect(String payFrom, String payTo, BigInteger pillId) {
	    String payFrom2 = payFrom;
        String payTo2 = payTo;
        if (payTo == "") {
        	    payTo2 = "empty";
        }
        if (payFrom == "") {
        	    payFrom2 = "empty";
        }
		try {
			String contractAddress = loadAssetAddr();
  
			Asset asset = Asset.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
			TransactionReceipt receipt = asset.select(payFrom2, payTo2, pillId).send();
			Tuple3<List<byte[]>, List<byte[]>, List<BigInteger>> response = asset.getSelectOutput(receipt);

			if (!response.getValue1().isEmpty()) {
				System.out.printf("select pill succeed \n");
				return response;
			}else {
				System.out.printf(" select pill failed");
			}

		} catch (Exception e) {
			logger.error(" select pill exception, error message is {}", e.getMessage());

			System.out.printf(" select pill  failed, error message is %s\n", e.getMessage());


		}
		List<byte[]> num1 = new ArrayList<byte[]>();
		List<byte[]> num2 = new ArrayList<byte[]>();
		List<BigInteger> num3 = new ArrayList<BigInteger>();
		return new Tuple3<List<byte[]>, List<byte[]>, List<BigInteger>>(num1,num2,num3);
	}

	public BigInteger pillInsert(String payFrom, String payTo, BigInteger payAccount) {
		try{
			String contractAddress = loadAssetAddr();
  
			Asset asset = Asset.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
			TransactionReceipt receipt = asset.insert(payFrom, payTo, payAccount).send();
			Tuple2<BigInteger, BigInteger> response = asset.getInsertOutput(receipt);

			if (response.getValue1().compareTo(new BigInteger("1")) == 0) {
				System.out.printf("insert pill succeed \n");
				return new BigInteger("1");
			}else {
				System.out.printf(" insert pill asset failed");
			}

		} catch (Exception e) {
			logger.error(" insert pill exception, error message is {}", e.getMessage());

			System.out.printf(" insert pill asset failed, error message is %s\n", e.getMessage());

		}
		return new BigInteger("0");
	}

	public BigInteger removeAccountAsset(String assetAccount) {

		try{
			String contractAddress = loadAssetAddr();
  
			Asset asset = Asset.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
			TransactionReceipt receipt = asset.assetRemoveAccount(assetAccount).send();
			Tuple2<BigInteger, BigInteger> response = asset.getAssetRemoveAccountOutput(receipt);
			if (response.getValue1().compareTo(new BigInteger("1")) == 0) {
				System.out.printf("removeAccount succeed \n");
				return new BigInteger("1");

			}else {
				System.out.printf(" removeAccount asset failed");
			}

		} catch (Exception e) {
			logger.error(" removeAccountAsset exception, error message is {}", e.getMessage());

			System.out.printf(" removeAccount asset account failed, error message is %s\n", e.getMessage());
			
		}
		return new BigInteger("0");
	
	}
	public BigInteger queryAssetAmount(String assetAccount) {
	
		try {
			String contractAddress = loadAssetAddr();

			Asset asset = Asset.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
			Tuple2<BigInteger, BigInteger> result = asset.assetSelect(assetAccount).send();
			if (result.getValue1().compareTo(new BigInteger("0")) == 0) {
				System.out.printf(" asset account %s, value %s \n", assetAccount, result.getValue2());
				
				return result.getValue2();
			} else {
				System.out.printf(" %s asset account is not exist \n", assetAccount);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			logger.error(" queryAssetAmount exception, error message is {}", e.getMessage());

			System.out.printf(" query asset account failed, error message is %s\n", e.getMessage());
		}
	
		return new BigInteger("0");
			
	}

	public BigInteger registerAssetAccount(String assetAccount, BigInteger amount) {
		try {
			String contractAddress = loadAssetAddr();
			Asset asset = Asset.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
			TransactionReceipt receipt = asset.assetRegister(assetAccount, amount).send();
			List<RegisterEventEventResponse> response = asset.getRegisterEventEvents(receipt);
			if (!response.isEmpty()) {
				if (response.get(0).ret.compareTo(new BigInteger("0")) == 0) {
					System.out.printf(" register asset account success => asset: %s, value: %s \n", assetAccount,
							amount);
					return new BigInteger("1");
				
				} else {
					System.out.printf(" register asset account failed, ret code is %s \n",
							response.get(0).ret.toString());
				}
			} else {
				System.out.println(" event log not found, maybe transaction not exec. ");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();

			logger.error(" registerAssetAccount exception, error message is {}", e.getMessage());
			System.out.printf(" register asset account failed, error message is %s\n", e.getMessage());
		}
		return new BigInteger("0");

	}

	public BigInteger transferAsset(String fromAssetAccount, String toAssetAccount, BigInteger amount) {
	
		try {
			String contractAddress = loadAssetAddr();
			Asset asset = Asset.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
			TransactionReceipt receipt = asset.assetTransfer(fromAssetAccount, toAssetAccount, amount).send();
			List<TransferEventEventResponse> response = asset.getTransferEventEvents(receipt);
			if (!response.isEmpty()) {
				if (response.get(0).ret.compareTo(new BigInteger("0")) == 0) {
					System.out.printf(" transfer success => from_asset: %s, to_asset: %s, amount: %s \n",
							fromAssetAccount, toAssetAccount, amount);
					return new BigInteger("1");
					
				} else {
					System.out.printf(" transfer asset account failed, ret code is %s \n",
							response.get(0).ret.toString());
			
				}
			} else {
				System.out.println(" event log not found, maybe transaction not exec. ");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();

			logger.error(" registerAssetAccount exception, error message is {}", e.getMessage());
			System.out.printf(" register asset account failed, error message is %s\n", e.getMessage());
		}
		return new BigInteger("0");

	}

	public static void Usage() {
		System.out.printf("heer \n");
		System.out.println(" Usage:");
		System.out.println("\t java -cp conf/:lib/*:apps/* org.fisco.bcos.asset.client.AssetClient deploy");
		System.out.println("\t java -cp conf/:lib/*:apps/* org.fisco.bcos.asset.client.AssetClient query account");
		System.out.println(
				"\t java -cp conf/:lib/*:apps/* org.fisco.bcos.asset.client.AssetClient register account value");
		System.out.println(
				"\t java -cp conf/:lib/*:apps/* org.fisco.bcos.asset.client.AssetClient transfer from_account to_account amount");
		System.exit(0);
	}

	private void display() {
	 	Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int screenSize_width = (int)screenSize.getWidth();
		int screenSize_height = (int)screenSize.getHeight();
		frm.setLocation(100,100);
		frm.setSize(screenSize_width,screenSize_height);
		Dimension frm_size = frm.getSize();
		//frm.setBounds(100,100,300,300);

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		frm.setTitle("我的窗口");
		Container c = frm.getContentPane();
		//c.setBackground(Color.RED);
		frm.setLayout(null);


         
        JTextArea pill_info = new JTextArea("info",30,7);
        JScrollPane pill_info_scro = new JScrollPane(pill_info);
        pill_info_scro.setBounds(100,50,screenSize_width/2,screenSize_height/2);
        pill_info.setLineWrap(false);
        frm.add(pill_info_scro);


	    JButton asset_add = new JButton("转进");
		asset_add.setLocation(screenSize_width*2/3+250,screenSize_height*3/5-30-50);
		asset_add.setSize(100,40);
        frm.add(asset_add);

        asset_add_account.setBounds(screenSize_width*2/3+180,screenSize_height*3/5-100-50,100,40);
        frm.add(asset_add_account);
      
        asset_add_amount.setBounds(screenSize_width*2/3+320,screenSize_height*3/5-100-50,100,40);
        frm.add(asset_add_amount);

         asset_add.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
            	try {
            	    AssetClient client = new AssetClient();
		            client.initialize();
                    
                    asset_add_amount.setText(String.valueOf(client.addAsset(asset_add_account.getText(), new BigInteger(asset_add_amount.getText()))));
                } catch(Exception ae) {

                }
            }
        });
        JButton asset_remove_account = new JButton("删除用户");
        asset_remove_account.setLocation(screenSize_width*2/3+250,screenSize_height*4/5-30-50);
        asset_remove_account.setSize(100,40);
        frm.add(asset_remove_account);
        //btn.setBounds(100,100,100,40);

       
        asset_remove_account_account.setBounds(screenSize_width*2/3+180,screenSize_height*4/5-100-50,100,40);
        frm.add(asset_remove_account_account);
        
        asset_remove_account_amount.setBounds(screenSize_width*2/3+320,screenSize_height*4/5-100-50,100,40);
        frm.add(asset_remove_account_amount);

        asset_remove_account.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
            	try {
            	    AssetClient client = new AssetClient();
		            client.initialize();
                    
                    asset_remove_account_amount.setText(String.valueOf(client.removeAccountAsset(asset_remove_account_account.getText())));
                } catch(Exception ae) {

                }
            }
            //asset_remove_account_amount.setText("aaaaaaaa");
        });

        JButton asset_register = new JButton("注册用户");
        asset_register.setLocation(screenSize_width*2/3+250,screenSize_height/5-30-50);
        asset_register.setSize(100,40);
        frm.add(asset_register);

     
        asset_register_account.setBounds(screenSize_width*2/3+180,screenSize_height/5-100-50,100,40);
        frm.add(asset_register_account);
       
        asset_register_amount.setBounds(screenSize_width*2/3+320,screenSize_height/5-100-50,100,40);
        frm.add(asset_register_amount);

        asset_register.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
            	try {
            	    AssetClient client = new AssetClient();
		            client.initialize();
                    
                    asset_register_amount.setText(String.valueOf(client.registerAssetAccount(asset_register_account.getText(), new BigInteger(asset_register_amount.getText()))));
                } catch(Exception ae) {

                }
            }
        });

        JButton asset_transfer = new JButton("转帐");
        asset_transfer.setLocation(screenSize_width*2/3+250,screenSize_height-30-70);
        asset_transfer.setSize(100,40);
        frm.add(asset_transfer);

     
        asset_transfer_from_account.setBounds(screenSize_width*2/3+180,screenSize_height-100-70,100,40);
        frm.add(asset_transfer_from_account);
       
        asset_transfer_to_account.setBounds(screenSize_width*2/3+320,screenSize_height-100-70,100,40);
        frm.add(asset_transfer_to_account);
      
        asset_transfer_amount.setBounds(screenSize_width*2/3+450,screenSize_height-100-70,100,40);
        frm.add(asset_transfer_amount);

        asset_transfer.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
            	try {
            	    AssetClient client = new AssetClient();
		            client.initialize();
                    
                    asset_transfer_amount.setText(String.valueOf(client.transferAsset(asset_transfer_from_account.getText(), asset_transfer_to_account.getText(),  new BigInteger(asset_transfer_amount.getText()))));
                } catch(Exception ae) {

                }
            }
        });

        JButton asset_select = new JButton("查询用户");
        asset_select.setLocation(screenSize_width*2/3+250,screenSize_height*2/5-30-50);
        asset_select.setSize(100,40);
        frm.add(asset_select);

      
        asset_select_account.setBounds(screenSize_width*2/3+180,screenSize_height*2/5-100-50,100,40);
        frm.add(asset_select_account);
    
        asset_select_amount.setBounds(screenSize_width*2/3+320,screenSize_height*2/5-100-50,100,40);
        frm.add(asset_select_amount);
        asset_select.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
            	try {
            	    AssetClient client = new AssetClient();
		            client.initialize();
                    
                    asset_select_amount.setText(String.valueOf(client.queryAssetAmount(asset_select_account.getText())));

                } catch(Exception ae) {

                }
     
            }
        });
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        JButton pill_insert = new JButton("插入单据");
        pill_insert.setBounds(180,screenSize_height*2/3+120,100,40);
        frm.add(pill_insert);
        JTextField pill_insert_from = new JTextField("用户名from",30);
        pill_insert_from.setBounds(60,screenSize_height*2/3+40,100,40);
        frm.add(pill_insert_from);
        JTextField pill_insert_to = new JTextField("用户名to",30);
        pill_insert_to.setBounds(60,screenSize_height*2/3+120,100,40);
        frm.add(pill_insert_to);
        JTextField pill_insert_amount = new JTextField("数额",30);
        pill_insert_amount.setBounds(60,screenSize_height*2/3+200,100,40);
        frm.add(pill_insert_amount);
        pill_insert.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
            	try {
            	    AssetClient client = new AssetClient();
		            client.initialize();
                    
                    pill_insert_amount.setText(String.valueOf(client.pillInsert(pill_insert_from.getText(), pill_insert_to.getText(), new BigInteger(pill_insert_amount.getText()))));
                } catch(Exception ae) {

                }
            }
        });

        JButton pill_change_pill = new JButton("转移单据");
        pill_change_pill.setBounds(500,screenSize_height*2/3+120,100,40);
        frm.add(pill_change_pill);
        JTextField pill_change_pill_to = new JTextField("用户名to",30);
        pill_change_pill_to.setBounds(380,screenSize_height*2/3+80,100,40);
        frm.add(pill_change_pill_to);
        JTextField pill_change_pill_id = new JTextField("单据id",30);
        pill_change_pill_id.setBounds(380,screenSize_height*2/3+160,100,40);
        frm.add(pill_change_pill_id);
        pill_change_pill.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
            	try {
            	    AssetClient client = new AssetClient();
		            client.initialize();
                    
                    pill_change_pill_id.setText(String.valueOf(client.pillTransferPill(pill_change_pill_to.getText(), new BigInteger(pill_change_pill_id.getText()))));
                } catch(Exception ae) {

                }
            }
        });
      
        JButton pill_select = new JButton("查询单据");
        pill_select.setBounds(screenSize_width/2-40,screenSize_height*2/3-70,100,40);
        frm.add(pill_select);
        JTextField pill_select_from = new JTextField("用户名from",30);
        pill_select_from.setBounds(screenSize_width/2-400,screenSize_height*2/3-70,100,40);
        frm.add(pill_select_from);
        JTextField pill_select_to = new JTextField("用户名to",30);
        pill_select_to.setBounds(screenSize_width/2-280,screenSize_height*2/3-70,100,40);
        frm.add(pill_select_to);
        JTextField pill_select_id = new JTextField("单据id",30);
        pill_select_id.setBounds(screenSize_width/2-160,screenSize_height*2/3-70,100,40);
        frm.add(pill_select_id);
        
        pill_select.addActionListener(new ActionListener(){
        	Tuple3<List<byte[]>, List<byte[]>, List<BigInteger>> num;
            public void actionPerformed(ActionEvent e){
            	try {
            		String long_info = "";
            	    AssetClient client = new AssetClient();
		            client.initialize();
                    
                    num = client.pillSelect( pill_select_from.getText(), pill_select_to.getText(), new BigInteger(pill_select_id.getText()));
                    if (num.getValue1().isEmpty()) {
                    	long_info = " There is no Bill!!! ";
                    }else {
                    	for(int i = 0;i < num.getValue1().size();i++) {
                    		String num1 = new String(num.getValue1().get(i));
                    		String num2 = new String(num.getValue2().get(i));
                    		String num3 = String.valueOf(num.getValue3().get(i));

                    		long_info += String.valueOf(i) + ".  The Bill is from : " + num1 + " is to : " +num2 + " amount : " + num3 + " \n";
                    	}
                    }

                    pill_info.setText(long_info);
                } catch(Exception ae) {

                }
            }
        });




        JButton pill_select_for_id = new JButton("查询单据ID");
        pill_select_for_id.setBounds(820,screenSize_height*2/3+120,100,40);
        frm.add(pill_select_for_id);
        JTextField pill_select_for_id_from = new JTextField("用户名from",30);
        pill_select_for_id_from.setBounds(700,screenSize_height*2/3+40,100,40);
        frm.add(pill_select_for_id_from);
        JTextField pill_select_for_id_to = new JTextField("用户名to",30);
        pill_select_for_id_to.setBounds(700,screenSize_height*2/3+120,100,40);
        frm.add(pill_select_for_id_to);
        JTextField pill_select_for_id_amount = new JTextField("欠款",30);
        pill_select_for_id_amount.setBounds(700,screenSize_height*2/3+200,100,40);
        frm.add(pill_select_for_id_amount);
        pill_select_for_id.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
            	try {
            	    AssetClient client = new AssetClient();
		            client.initialize();
                    
                    pill_select_for_id_amount.setText(String.valueOf(client.pillSelectForId(pill_select_for_id_from.getText(), pill_select_for_id_to.getText(), new BigInteger(pill_select_for_id_amount.getText()) )));
                } catch(Exception ae) {

                }
            }
        });

        JButton pill_delete_pill = new JButton("删除单据");
        pill_delete_pill.setBounds(1140,screenSize_height*2/3+120,100,40);
        frm.add(pill_delete_pill);
        JTextField pill_delete_pill_from = new JTextField("用户名from",30);
        pill_delete_pill_from.setBounds(1020,screenSize_height*2/3+20,100,40);
        frm.add(pill_delete_pill_from);
        JTextField pill_delete_pill_to = new JTextField("用户名to",30);
        pill_delete_pill_to.setBounds(1020,screenSize_height*2/3+80,100,40);
        frm.add(pill_delete_pill_to);
        JTextField pill_delete_pill_amount = new JTextField("数额",30);
        pill_delete_pill_amount.setBounds(1020,screenSize_height*2/3+140,100,40);
        frm.add(pill_delete_pill_amount);
        JTextField pill_delete_pill_id = new JTextField("单据id",30);
        pill_delete_pill_id.setBounds(1020,screenSize_height*2/3+200,100,40);
        frm.add(pill_delete_pill_id);
        pill_delete_pill.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
            	try {
            	    AssetClient client = new AssetClient();
		            client.initialize();
                    
                    pill_delete_pill_amount.setText(String.valueOf(client.pillDeletePill(pill_delete_pill_from.getText(), pill_delete_pill_to.getText(), new BigInteger(pill_delete_pill_amount.getText()), new BigInteger(pill_delete_pill_id.getText()) )));
                } catch(Exception ae) {

                }
            }
        });

        JButton pill_check = new JButton("查询用户经济能力");
        pill_check.setBounds(1280,screenSize_height*2/3+160,100,40);
        frm.add(pill_check);
        JTextField pill_check_account = new JTextField("用户名to",30);
        pill_check_account.setBounds(1280,screenSize_height*2/3+80,100,40);
        frm.add(pill_check_account);
        pill_check.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
            	try {
            	    AssetClient client = new AssetClient();
		            client.initialize();
                    
                    pill_check_account.setText(String.valueOf(client.pillCheck(pill_check_account.getText())));
                } catch(Exception ae) {

                }
            }
        });
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        frm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frm.setVisible(true);
	 }

	public static void main(String[] args) throws Exception {
		AssetClient window = new AssetClient();
        
		if (args.length < 1) {
			Usage();
		}

		AssetClient client = new AssetClient();
		client.initialize();
        
		switch (args[0]) {
		case "deploy":
			client.deployAssetAndRecordAddr();
			System.exit(0);
			break;
		case "start":
		    window.display();
		    break;
		case "query":
			if (args.length < 2) {
				Usage();
			}
			window.display();
			//client.queryAssetAmount(args[1]);
			break;
		case "register":
			if (args.length < 3) {
				Usage();
			}
			client.registerAssetAccount(args[1], new BigInteger(args[2]));
			System.exit(0);
			break;
		case "transfer":
			if (args.length < 4) {
				Usage();
			}
			client.transferAsset(args[1], args[2], new BigInteger(args[3]));
			System.exit(0);
			break;
		case "add":
		    if (args.length < 3) {
		    	Usage();
		    }
		    client.addAsset(args[1], new BigInteger(args[2]));
		    System.exit(0);
		    break;
		case "removeAccount":
		    if (args.length < 2) {
		    	Usage();
		    }
		    client.removeAccountAsset(args[1]);
		    System.exit(0);
		    break;
		default: {
			Usage();
		}
		}

		//System.exit(0);
	}
}
