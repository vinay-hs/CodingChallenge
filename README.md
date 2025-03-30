Approach

This project follows a structured approach to retrieve and analyze Ethereum account information:
Fetching ETH Balance
Uses Web3j and Infura to fetch the account balance in Wei.
Converts it to ETH and calculates its USD equivalent using CoinGecko.

Fetching Transaction History
Calls the Etherscan API to retrieve all transactions of the given address.
Filters transactions related to Uniswap and SushiSwap swaps.
Calculates the total transaction volume in ETH.

Fetching account balance of all assets in USD(ERC-20 Token Balances)
Calls Etherscan API to retrieve ERC-20 token balances for the given address.
Uses CoinGecko API to fetch token prices in USD.
Displays token balances and their equivalent values in USD.

Handling API Rate Limits and Errors
Implements exception handling for API errors (e.g., HTTP 429 Too Many Requests).
Introduces a retry mechanism or delay if API requests are throttled.

How to Run the Code:
Prerequisites::
Java 8 or later installed,
Maven installed, 
Eclipse or any Java IDE, 
Git installed.

Steps to Run:: 
1. Clone the Repository,
2. Navigate to the Project Directory,
3. Build the Project,
4. Run the Application.
