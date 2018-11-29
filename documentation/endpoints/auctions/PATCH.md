# Update an auction

## Description

This endpoint allow you to update an existing auction.

## Authentication

No authentication required

## Path

<code>PATCH</code> /auctions/:item

## Headers

```
Content-type: application/json
```

## Url parameters

This endpoint does not accept url parameters.

## Request body

This endpoint must have a well-formatted json body:
```
{
	"startingPrice": 300,
	"incrementPolicy": {
		"key": "FreeIncrement"
	},
	"startDate": 1543239508000,
	"endDate": 1553239508000
}
```

- item is optional, but must be a string if present
- incrementPolicy is optional, but if present, must be well-formed:
    - key must be present and correspond to an existing increment policy key ("FreeIncrement", "MinimalIncrement")
    - min must be present only if the increment policy key is "MinimalIncrement"
- startingPrice is optional but must be a positive integer representing the starting price in cents if present
- startDate is optional but must be a unix timestamp expressed in milliseconds if present
- endDate is optional but must be a unix timestamp expressed in milliseconds if present

## Success response

In order for the request to be successful, the :item field in the url must correspond to an already created option with a "Planned" state.

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

- Code: <code>400 BAD REQUEST</code>
- Headers:
    ```
    Content-type: text/plain; charset=UTF-8
    ```
- Content:
    ```
    The request content was malformed: [A descriptive message]
    ```
- When:
    - The request was malformed
    
OR

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
    
OR

- Code: <code>422 UNPROCESSABLE ENTITY</code>
- Headers:
    ```
    Content-type: application/json
    ```
- Content:
    ```
    {
    	"message": "The provided starting price '-200' is negative"
    }
    ```
- When:
    - The startingPrice parameter was negative
    
OR

- Code: <code>423 LOCKED</code>
- Headers:
    ```
    Content-type: application/json
    ```
- Content:
    ```
    {
    	"message": "The actual auction state 'Opened' doesn't allow this action"
    }
    ```
- When:
    - The state of the auction is not "Planned"