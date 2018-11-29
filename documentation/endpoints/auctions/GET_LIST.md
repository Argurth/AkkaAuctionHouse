# Get the list of auctions

## Description

This endpoint allow you to retrieve the list of auctions

## Authentication

No authentication required

## Path

<code>GET</code> /auctions

## Headers

This endpoint does not need any particular header

## Url parameters

This endpoint does not accept url parameters.

## Request body

This endpoint does not accept a body

## Success response

The server will answer with a list of auction (empty if no auction exists on the server):

- Code: <code>200 OK</code>
- Headers:
    ```
    Content-type: application/json
    ```
- Content: 
    ```
    [
    	{
    		"auctionState": "Opened",
    		"endDate": 1553239508000,
    		"bids": [],
    		"startingPrice": 300,
    		"incrementPolicy": {
    			"key": "FreeIncrement"
    		},
    		"startDate": 1543239508000,
    		"bidders": [],
    		"item": "test"
    	}
    ]
    ```
    
## Error responses

Apart from unexpected errors, no error should be raised by this endpoint