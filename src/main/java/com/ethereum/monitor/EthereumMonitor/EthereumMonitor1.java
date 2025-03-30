package com.ethereum.monitor.EthereumMonitor;

import org.json.JSONArray;
import org.json.JSONObject;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.utils.Convert;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EthereumMonitor1 {
    private static final String INFURA_URL = "https://mainnet.infura.io/v3/699c317910c34bca8b75c3c06e446d28";
    private static final String ETHERSCAN_API_KEY = "94J4S4G3SDQ8Z53RVNNBCBVJ3E2W9RZKSX";
    private static final String ETHERSCAN_API_URL = "https://api.etherscan.io/api";
    private static final String COINGECKO_API_URL = "https://api.coingecko.com/api/v3/simple/price?ids=ethereum&vs_currencies=usd";
    private static final String ADDRESS_TO_MONITOR = "0x4838B106FCe9647Bdf1E7877BF73cE8B0BAD5f97"; 
    private static final String COINGECKO_TOKEN_LIST_API = "https://api.coingecko.com/api/v3/coins/list";
    private static Map<String, String> tokenMap = new HashMap<>();

    public static void main(String[] args) {
        try {
            System.out.println("Monitoring Ethereum Address: " + ADDRESS_TO_MONITOR);

            // Load token mappings dynamically
            loadTokenMappings();

            // Fetch ETH balance
            double ethBalance = getAccountBalance();
            double ethPrice = getEthPriceInUSD();
            double balanceInUSD = ethBalance * ethPrice;
            System.out.println("ETH Balance: " + ethBalance + " ETH (~$" + balanceInUSD + " USD)");

            // Fetch historical transactions
            JSONArray transactions = fetchTransactionHistory();
            int limit = Math.min(transactions.length(), 10);  // Ensure we don't exceed available transactions
            System.out.println("Showing up to 10 Transactions:");
            for (int i = 0; i < limit; i++) {
                System.out.println(transactions.getJSONObject(i));  // Print each transaction
            }

            // Detect swaps (Uniswap, SushiSwap)
            int swapCount = countSwapTransactions(transactions);
            System.out.println("Swap Transactions (Uniswap, SushiSwap): " + swapCount);

            // Calculate total transaction volume
            double totalVolume = calculateTransactionVolume(transactions);
            double totalVolumeUSD = totalVolume * ethPrice;
            System.out.println("Total Transaction Volume: " + totalVolume + " ETH (~$" + totalVolumeUSD + " USD)");

            // Fetch and display ERC-20 token balances in USD
            fetchAndDisplayERC20TokenBalances();

            // Adding a small delay to keep the program running and to view the output
            Thread.sleep(5000); // Delay for 5 seconds

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }

    // Fetch Ethereum account balance
    public static double getAccountBalance() throws Exception {
        Web3j web3 = Web3j.build(new HttpService(INFURA_URL));
        EthGetBalance balanceWei = web3.ethGetBalance(ADDRESS_TO_MONITOR, org.web3j.protocol.core.DefaultBlockParameterName.LATEST).send();
        BigInteger weiBalance = balanceWei.getBalance();
        return Convert.fromWei(weiBalance.toString(), Convert.Unit.ETHER).doubleValue();
    }

    // Function to Fetch historical transactions from Etherscan
    public static JSONArray fetchTransactionHistory() throws IOException {
        String url = ETHERSCAN_API_URL + "?module=account&action=txlist&address=" + ADDRESS_TO_MONITOR +
                     "&startblock=0&endblock=99999999&sort=asc&apikey=" + ETHERSCAN_API_KEY;
        String response = sendHttpRequest(url);
        JSONObject jsonResponse = new JSONObject(response);
        return jsonResponse.optJSONArray("result");
    }

    // Function for Count swap transactions (Uniswap, SushiSwap)
    public static int countSwapTransactions(JSONArray transactions) {
        List<String> swapContracts = Arrays.asList(
            "0x5C69bEe701ef814a2B6a3EDD4B1652CB9cc5aA6f", // Uniswap V2 Factory
            "0xC0AeE478e3658e2610c5F7A4A2E1777cE9e4f2Ac", // Uniswap V2 Router
            "0xd9e1cE17f2641f24aE83637ab66a2cca9C378B9F"  // SushiSwap Router
        );

        int swapCount = 0;
        for (int i = 0; i < transactions.length(); i++) {
            JSONObject tx = transactions.getJSONObject(i);
            String toAddress = tx.optString("to").toLowerCase();
            if (swapContracts.contains(toAddress)) {
                swapCount++;
            }
        }
        return swapCount;
    }

    // Function to Calculate total transaction volume in ETH
    public static double calculateTransactionVolume(JSONArray transactions) {
        double totalVolumeETH = 0;
        for (int i = 0; i < transactions.length(); i++) {
            JSONObject tx = transactions.getJSONObject(i);
            double ethAmount = tx.optDouble("value") / 1e18; // Convert Wei to ETH
            totalVolumeETH += ethAmount;
        }
        return totalVolumeETH;
    }

    // Function to Fetch ETH price in USD from CoinGecko
    public static double getEthPriceInUSD() throws IOException {
        String response = sendHttpRequest(COINGECKO_API_URL);
        JSONObject jsonResponse = new JSONObject(response);
        return jsonResponse.getJSONObject("ethereum").getDouble("usd");
    }

    // Function to Fetch and display ERC-20 token balances in USD
    public static void fetchAndDisplayERC20TokenBalances() throws IOException {
        String url = ETHERSCAN_API_URL + "?module=account&action=tokentx&address=" +
                     ADDRESS_TO_MONITOR + "&startblock=0&endblock=99999999&sort=asc&apikey=" + ETHERSCAN_API_KEY;
        String response = sendHttpRequest(url);
        JSONObject jsonResponse = new JSONObject(response);
        JSONArray tokenTransactions = jsonResponse.optJSONArray("result");

        if (tokenTransactions == null || tokenTransactions.length() == 0) {
            System.out.println(" No ERC-20 token transactions found.");
            return;
        }

        System.out.println(" ERC-20 Token Transactions:");
        for (int i = 0; i < tokenTransactions.length(); i++) {
            JSONObject tokenTx = tokenTransactions.getJSONObject(i);
            String tokenSymbol = tokenTx.optString("tokenSymbol");
            double tokenAmount = Double.parseDouble(tokenTx.optString("value")) / Math.pow(10, Integer.parseInt(tokenTx.optString("tokenDecimal")));

            String coinGeckoId = getCoinGeckoId(tokenSymbol);
            if (!coinGeckoId.isEmpty()) {
                double tokenPrice = fetchTokenPrice(coinGeckoId);
                double tokenAmountInUSD = tokenAmount * tokenPrice;
                System.out.println( tokenSymbol + ": " + tokenAmount + " (~$" + tokenAmountInUSD + " USD)");
            } else {
                System.out.println(" No price data for token: " + tokenSymbol);
            }
        }
    }

    // Function to Fetch token price dynamically from CoinGecko
    public static double fetchTokenPrice(String coinGeckoId) throws IOException {
        String url = "https://api.coingecko.com/api/v3/simple/price?ids=" + coinGeckoId + "&vs_currencies=usd";
        String response = sendHttpRequest(url);
        JSONObject jsonResponse = new JSONObject(response);
        return jsonResponse.getJSONObject(coinGeckoId).optDouble("usd", 0);
    }

    // Function to Fetch all token symbols and IDs dynamically from CoinGecko
    public static void loadTokenMappings() throws IOException {
        String response = sendHttpRequest(COINGECKO_TOKEN_LIST_API);
        JSONArray tokenList = new JSONArray(response);

        for (int i = 0; i < tokenList.length(); i++) {
            JSONObject token = tokenList.getJSONObject(i);
            String id = token.getString("id");
            String symbol = token.getString("symbol").toUpperCase();
            tokenMap.put(symbol, id);
        }
    }

    public static String getCoinGeckoId(String symbol) {
        return tokenMap.getOrDefault(symbol.toUpperCase(), "");
    }

    // Function to Handle Rate Limiting
    public static String sendHttpRequest(String url) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("GET");

        int retryCount = 0;
        int maxRetries = 5;
        int retryDelay = 5000; // 5 seconds delay between retries

        while (retryCount < maxRetries) {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                return response.toString(); // If successful, return the response
            } catch (IOException e) {
                if (con.getResponseCode() == 429) {
                    System.out.println(" Rate limit exceeded. Retrying in " + retryDelay / 1000 + " seconds...");
                    retryCount++;
                    try {
                        Thread.sleep(retryDelay); // Wait before retrying
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    throw e; // If it's another error, throw it
                }
            }
        }

        throw new IOException("Max retries reached. Could not fetch data.");
    }
}
