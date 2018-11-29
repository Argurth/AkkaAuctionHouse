# Get an auction

## Description

This endpoint allow you to get an existing auction.

## Authentication

No authentication required

## Path

<code>GET</code> /auctions/:item

## Headers

This endpoint does not need any particular header

## Url parameters

This endpoint does not accept url parameters.

## Request body

This endpoint does not accept a body

## Success response

In order for the request to be successful, the :item field in the url must correspond to an already created option.

In which case the server will answer:

- Code: <code>200 OK</code>
- Headers:
    ```
    Content-type: application/json
    ```
- Content: 
    ```
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
    ```
    
## Error responses

If the request is unsuccessful, the server may responds with the following errors:

- Code: <code>404 NOT FOUND</code>
- Headers:
    ```
    Content-type: application/json
    ```
- Content:
    ```
    {
    	"message": "The auction 'test2' doesn't exist"
    }
    ```
- When:
    - The item parameter in route does not match an existing auction