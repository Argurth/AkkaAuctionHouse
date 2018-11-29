# Place a bid on an auction

## Description

This endpoint allow you to place a bid on an auction

## Authentication

No authentication required

## Path

<code>POST</code> /auctions/:item/bidders/:bidderName/bid

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
	"value": 800
}
```

- value must be present, must be an integer representing the bid price in cents and must be valid according to the current bid policy

## Success response

In order for the request to be successful, the :item field in the url must correspond to an already created option with an "Opened" state and the :bidderName should correspond to the name of a bidder previously added to this auction

In which case the server will answer:

- Code: <code>201 CREATED</code>
- Headers:
    ```
    Content-type: application/json
    ```
- Content: 
    ```
    {
    	"bidderName": "testuser",
    	"value": 800
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

- Code: <code>404 NOT FOUND</code>
- Headers:
    ```
    Content-type: application/json
    ```
- Content:
    ```
    {
    	"message": "'testuser2' hasn't joined this auction"
    }
    ```
- When:
    - The provided bidderName does not match any user added to this auction
    
OR

- Code: <code>422 UNPROCESSABLE ENTITY</code>
- Headers:
    ```
    Content-type: application/json
    ```
- Content:
    ```
    {
    	"message": "The bid '100' is too low for this auction"
    }
    ```
- When:
    - The provided bid value is too low according to the increment policy of the auction
    
OR

- Code: <code>423 LOCKED</code>
- Headers:
    ```
    Content-type: application/json
    ```
- Content:
    ```
    {
    	"message": "The actual auction state 'Closed' doesn't allow this action"
    }
    ```
- When:
    - The state of the auction is not "Opened"