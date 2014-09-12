# Grayshirts Playframework Modules #

Modules for adding functionality to Play Framework 1.2.x

### Available modules ###


```
#!java


    - ar.com.grayshirts -> gzip 0.2


```

### Available modules ###
Remember to add grayshirts repository to download dependencies
```
#!java


repositories:
    - grayshirts:
        type: http
        artifact: "https://bytebucket.org/grayshirts/play-modules/raw/b9f3505c59ed14388c2767f432297fd2a7d12001/[module]/dist/[module]-[revision].zip"
        contains:
            - ar.com.grayshirts -> *

```


---------------------------


### Coming soon modules  ###

* Playframework updater (netty, groovy, etc)
* Static resources minifier
* Better CRUDs for MongoDB
* Cache optimizations

### Contribution guidelines ###

Feel free to use, comment or suggest enhancements for these modules.



