package com.eth.web.web3j.send;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.*;
import org.web3j.protocol.ObjectMapperFactory;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.ChainId;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.List;

    /**
     * 冷钱包
     * 账号 交易相关
     * create by ： Hf
     *
     * desc :  本程序使用私钥对所有交易签名 绝密文件  禁止外泄
     *         限制内网访问接口地址为：http://192.168.1.251:8000/
     *         java -jar 直接提供服务
     *         另：禁止钱包开放8545端口
     *
     *         测试以及前期使用阶段统计交易数据 对比查账
     */
    //TODO  考虑REST 风格接口
    //TODO  待测试改为HTTP_PUT  测试保证绝对的幂等性 不建议HTTP_POST
    @Controller
    public class EthWeb3j {
        private static final Logger logger = LoggerFactory.getLogger(EthWeb3j.class);
        private Web3j web3j = Web3j.build(new HttpService("http://192.168.1.236:28545"));

        private String address = "0xa530d89646db11abfa701e148e87324355fc6ea7";
        private static String ethpassword = "hf";
        private static String keystore = "{\"address\":\"a530d89646db11abfa701e148e87324355fc6ea7\",\"id\":\"246e7d1d-8f31-4a3e-951d-41722213a44f\",\"version\":3,\"crypto\":{\"cipher\":\"aes-128-ctr\",\"ciphertext\":\"26d10977bc199f6b678e89d3b7c3874bab3cddda18b92c014890d80657d7cc6a\",\"cipherparams\":{\"iv\":\"beaa9a404f793e86460a1fc71a0372a8\"},\"kdf\":\"scrypt\",\"kdfparams\":{\"dklen\":32,\"n\":262144,\"p\":1,\"r\":8,\"salt\":\"f06eb3d208db1643671c6e0210789f05e6de1746252fe5b83a38618e2bd18f1e\"},\"mac\":\"0aa4f85dfecaf8203ad0ee22c47ff6fb35b8f47d8f56ddb890ef2d513a06a801\"}}\n";
        private static String privateKey = "f4529331f460fa88cc14eb981baf90201e7fc709386bf2f5b9ec687639f70086";
        private static String privateKey1 ="8bdbdc0202e1209c58ba86a92a8082d929f78783052c995ff0792e8797f6dd3c";  //mask
        private static String privatekeytestnet= "899c3b97da9bb068c63cde10ffbe955def10808470e9c25073104d3bc9f9479";// testnet
        private static String privatakey2 ="can erosion creek laundry type nurse custom robust raccoon like spin track"; //助记词


        /**
         * @DESC 访问接口人员必须要求验证给定密码 （没必要一定是钱包的密码）
         * @param password
         * @return
         */
        @RequestMapping(value = "eth/verifyPassword",method = RequestMethod.GET)
        @ResponseBody
        public String verifyPassword(String password){
            String result = "false";
            if (password != null&&password.equals(ethpassword))
                result = "true";
            return  result;

        }

        /**
         * 获取nonce
         * @param address
         * @return
         */
        @RequestMapping(value = "/eth/getEthNonce",method = RequestMethod.GET)
        @ResponseBody
        public BigInteger getNonce(String address){
            BigInteger nonce =BigInteger.valueOf(-1);
            EthGetTransactionCount ethGetTransactionCount = null;
            try {
                ethGetTransactionCount = web3j.ethGetTransactionCount(address, DefaultBlockParameterName.PENDING).send();
            } catch (IOException e) {
                logger.error("get nonce fail because {}",e.getMessage());
            }
            if (ethGetTransactionCount == null)
                return nonce;
            nonce = ethGetTransactionCount.getTransactionCount();
            return  nonce;
        }


        /**
         * 离线签名
         * @description  gas默认4Gwei
         *               已优化为动态获取上一个块的平均值 避免以太坊拥堵
         * @param to
         * @param amount
         * @return
         */
        //String to = "0xb4d025dea4072879ffd1fb2f345cbc61dc4a11a2".toLowerCase();
        @RequestMapping(value = "eth/ethSendRawTransaction",method = RequestMethod.GET)
        @ResponseBody
        public String  testTransaction(String to,String amount) {
            String txId = null;
            BigInteger nonce;
            EthGasPrice ethGasPrice;
            BigInteger gasPrice = Convert.toWei(BigDecimal.valueOf(4), Convert.Unit.GWEI).toBigInteger();
            EthGetTransactionCount ethGetTransactionCount = null;
            try {
                ethGetTransactionCount = web3j.ethGetTransactionCount(address, DefaultBlockParameterName.PENDING).send();
            } catch (IOException e) {
                logger.error("get nonce fail because {}",e.getMessage());
            }
            if (ethGetTransactionCount == null)
                return txId;
            nonce = ethGetTransactionCount.getTransactionCount();
            try{
                ethGasPrice = web3j.ethGasPrice().send();
                gasPrice = ethGasPrice.getGasPrice();
                System.out.println(gasPrice);
            }catch (Exception e){
                logger.error("get gasPrice fail because {}",e.getMessage());
            }
            BigInteger gasLimit = BigInteger.valueOf(30000);
            BigInteger value = Convert.toWei( new BigDecimal(amount), Convert.Unit.ETHER).toBigInteger();
            String data = "";
            byte chainId = ChainId.ROPSTEN;//测试网络
            String privateKey = EthWeb3j.privatekeytestnet;
            String signedData;
            try {
                signedData = signTransaction(nonce, gasPrice, gasLimit, to, value, data, chainId, privateKey);
                logger.info("signedData"+signedData);
                if (signedData != null) {
                    EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(signedData).send();
                    System.out.println(ethSendTransaction.getTransactionHash());
                    txId = ethSendTransaction.getTransactionHash();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return  txId;
        }

        /**
         * 发送erc20代币
         * @Desc 主网gaslimit 设为90000
         *      TODO 查看gaslimit
         *
         */
        @RequestMapping(value = "eth/erc20SendRawTransaction",method = RequestMethod.GET)
        @ResponseBody
        private  String sendTokenTransaction(String fromAddress, String privateKey, String contractAddress, String toAddress, double amount, int decimals) {
           String txId = "";
            BigInteger nonce;
            BigInteger gasPrice= Convert.toWei(BigDecimal.valueOf(4), Convert.Unit.GWEI).toBigInteger();
            EthGetTransactionCount ethGetTransactionCount = null;
            try {
                ethGetTransactionCount = web3j.ethGetTransactionCount(fromAddress, DefaultBlockParameterName.PENDING).send();
            } catch (IOException e) {
                logger.error("get nonce fail because {}",e.getMessage());
            }
            if (ethGetTransactionCount == null) return txId;
            nonce = ethGetTransactionCount.getTransactionCount();
            logger.info("nonce " + nonce);

            try {
                EthGasPrice ethGasPrice = web3j.ethGasPrice().send();
                gasPrice = ethGasPrice.getGasPrice();
                logger.info("gasPrice " + gasPrice);

            }catch (Exception e){
                logger.error("get gasPrice fail because {}",e.getMessage());
            }

            BigInteger gasLimit = BigInteger.valueOf(30000);
            BigInteger value = BigInteger.ZERO;
            //token转账参数
            String methodName = "transfer";
            List<Type> inputParameters = new ArrayList<>();
            List<TypeReference<?>> outputParameters = new ArrayList<>();
            Address tAddress = new Address(toAddress);
            Uint256 tokenValue = new Uint256(BigDecimal.valueOf(amount).multiply(BigDecimal.TEN.pow(decimals)).toBigInteger());
            inputParameters.add(tAddress);
            inputParameters.add(tokenValue);
            TypeReference<Bool> typeReference = new TypeReference<Bool>() {
            };
            outputParameters.add(typeReference);
            Function function = new Function(methodName, inputParameters, outputParameters);
            String data = FunctionEncoder.encode(function);

            byte chainId = ChainId.NONE;
            String signedData;
            try {
                signedData = EthWeb3j.signTransaction(nonce, gasPrice, gasLimit, contractAddress, value, data, chainId, privateKey);
                logger.info("signedData"+signedData);
                if (signedData != null) {
                    EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(signedData).send();
                    System.out.println(ethSendTransaction.getTransactionHash());
                    txId = ethSendTransaction.getTransactionHash();
                }
            } catch (IOException e) {
                logger.error("erc20 send fail -- {}",e.getMessage());
            }
            return  txId;
        }


        /**
         * 创建钱包
         *
         * @param password 密码
         */

        @RequestMapping(value = "eth/createWallet",method = RequestMethod.GET)
        @ResponseBody
        public  void createWallet(String password) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException, CipherException, JsonProcessingException {
            WalletFile walletFile;
            ECKeyPair ecKeyPair = Keys.createEcKeyPair();
            walletFile = Wallet.createStandard(password, ecKeyPair);
            logger.info("address " + walletFile.getAddress());
            ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();
            String jsonStr = objectMapper.writeValueAsString(walletFile);
            logger.info("keystore json file " + jsonStr);
        }


        /**
         * 查询eth余额
         * @param address
         * @return
         */
        //TODO  确定方案 1.遍历所有地址 余额相+  2. 汇总入此账户地址  待调用转账方案

        @RequestMapping(value = "eth/getEthBalance",method = RequestMethod.GET)
        @ResponseBody
        public  BigDecimal getEthBalance (String address){
            BigDecimal balance = BigDecimal.ONE;
            System.out.println(web3j.ethGetBalance(address,DefaultBlockParameterName.LATEST));
            return  balance;
        }


        /**
         * 查询代币余额
         */

        @RequestMapping(value = "eth/getErc20Balance",method = RequestMethod.GET)
        @ResponseBody
        public  BigInteger getTokenBalance(String fromAddress, String contractAddress) {

            String methodName = "balanceOf";
            List<Type> inputParameters = new ArrayList<>();
            List<TypeReference<?>> outputParameters = new ArrayList<>();
            Address address = new Address(fromAddress);
            inputParameters.add(address);

            TypeReference<Uint256> typeReference = new TypeReference<Uint256>() {
            };
            outputParameters.add(typeReference);
            Function function = new Function(methodName, inputParameters, outputParameters);
            String data = FunctionEncoder.encode(function);
            Transaction transaction = Transaction.createEthCallTransaction(fromAddress, contractAddress, data);

            EthCall ethCall;
            BigInteger balanceValue = BigInteger.ZERO;
            try {
                ethCall = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send();
                List<Type> results = FunctionReturnDecoder.decode(ethCall.getValue(), function.getOutputParameters());
                balanceValue = (BigInteger) results.get(0).getValue();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return balanceValue;
        }


        /**
         * 已抽取签名方法
         * @throws IOException
         *
         */
        private static String signTransaction(BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit, String to,
                                              BigInteger value, String data, byte chainId, String privateKey) throws IOException {
            byte[] signedMessage;
            RawTransaction rawTransaction = RawTransaction.createTransaction(
                    nonce,
                    gasPrice,
                    gasLimit,
                    to,
                    value,
                    data);

            if (privateKey.startsWith("0x")) {
                privateKey = privateKey.substring(2);
            }
            ECKeyPair ecKeyPair = ECKeyPair.create(new BigInteger(privateKey, 16));
            Credentials credentials = Credentials.create(ecKeyPair);

            if (chainId > ChainId.NONE) {
                signedMessage = TransactionEncoder.signMessage(rawTransaction, chainId, credentials);
            } else {
                signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
            }

            String hexValue = Numeric.toHexString(signedMessage);
            return hexValue;
        }
    }




