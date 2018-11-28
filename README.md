## Table of contents

- [Getting Started](#getting-started)
    - [Prerequisites](#prerequisites)
    - [Installing](#installing)
- [Running the tests](#running-the-tests)
- [Functionalities](#functionalities)
- [Documentation](#documentation)
    - [Insomnia](#insomnia)
    - [Endpoints](#endpoints)
        - [Auctions](#auctions)
- [Contributing](#contributing)
- [Authors](#authors)
- [License](#license)

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. 

### Prerequisites

You must have [git](https://git-scm.com/) installed and configured on your computer to clone this project.
- Install git : https://git-scm.com/downloads

You must have at least [java 8](https://www.oracle.com/technetwork/java/index.html) and [sbt](https://www.scala-sbt.org) installed on your computer in order to run this project.

- Get java: https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
- Get sbt: https://www.scala-sbt.org/1.0/docs/Setup.html

### Installing

Select a directory in which you will clone the repository:

```
$ cd ~/path/to/workspace
```

Clone the repository:

```
$ git clone git@github.com:Argurth/AkkaAuctionHouse.git local-project-name
$ cd local-project-name
```

Once in the project repository, you should be able to run the project with sbt by using the following command:

```
$ sbt run
```

The API server should start and be accessible at [http://localhost:5000](http://localhost:5000)

## Running the tests

This project uses [ScalaTest](http://www.scalatest.org/) as a test framework.

If you wish to execute the tests, use the following command:

```
$ sbt test
```

## Functionalities

This API give you access to different endpoints, which can be used to implement functionalities on a client application, including, but not limited to :

- Creating a new auction by specifying the item name, the starting price, the increment policy to use as well as the start and end dates of this auction.
- Updating the starting price, increment policy as well as the start and end date of an existing, planned, auction
- Getting the list of all the auctions as well as their properties, which can then be used by the client application to display:
    - The highest bid and the name of the bidder of those auctions
    - The bidding history of those auctions
    - The details (item, starting price, etc) of those auctions
    - etc
- Getting a particular auction using its item name
- Subscribing to an auction
- Placing a bid on an auction where the bidder is already subscribed

The API does not include, at the moment:
    - An authentication/authorization mechanism
    - A state persistence, on shutdown, all auctions are lost

## Documentation

### Insomnia

If you want to try the API without having to type all the requests by hand, an [insomnia](https://insomnia.rest/) export file, containing all the endpoints of this API, can be downloaded [here](/insomnia.json).

- If you haven't insomnia installed on your computer, you can download it here: https://insomnia.rest/download
- To import the data into insomnia, open the Application>Preference>Data menu, choose "Import Data"
- Select "From File" and import the file
- Then, using the environment selector, choose the development environment

You can also add/create a production environment if you decide to deploy the project

### Endpoints

#### Auctions

| Description | Request |
| -- | -- |
| Create an auction | **[<code>POST</code> /auctions](/documentation/endpoints/auctions/POST.md)** |
| Get the list of auctions | **[<code>GET</code> /auctions](/documentation/endpoints/auctions/GET_LIST.md)** |
| Get an auction | **[<code>GET</code> /auctions/:item](/documentation/endpoints/auctions/GET.md)** |
| Update an auction | **[<code>PATCH</code> /auctions/:item](/documentation/endpoints/auctions/PATCH.md)** |
| Subscribe to an auction | **[<code>POST</code> /auctions/:item/bidders](/documentation/endpoints/auctions/bidders/POST.md)** |
| Place a bid on an auction | **[<code>POST</code> /auctions/:item/bidders/:bidderName/bids](/documentation/endpoints/auctions/bidders/bids/POST.md)** |

## Contributing

If you want to contribute to this repository: 

First, create a branch for your feature:

```
$ git checkout -b my-awesome-feature
```

Commit your changes:

```
$ git commit -am 'Add an awesome feature'
```

Then, push the branch to the repository:

```
$ git push origin my-awesome-feature
```

Finally, submit a pull request, I will review it as soon as possible :)


## Authors

* **Julien GILSON** - *Initial work* - [Argurth](https://github.com/Argurth)

See also the list of [contributors](https://github.com/Argurth/AkkaAuctionHouse/graphs/contributors) who participated in this project.

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details