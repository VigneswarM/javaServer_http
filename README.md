HttpServer implementation

Download and get check
httpc get -v -h Accept:text/plain "http://127.0.0.1/post.json
httpc get -v -h Accept:text/plain "http://127.0.0.1/404.html

Post check
httpc post -v -h Accept:text/plain -f test.json "http://127.0.0.1/404
httpc post -v --d "{Assignment:1}" "http://127.0.0.1/post"

Security check
httpc post -v -h Accept:text/plain -f test.json "http://127.0.0.1/trick/get.json

Server command for normal web server

java -cp .;jopt-simple-4.6.jar;json-20140107.jar;gson-2.6.2.jar HttpServerMain -n true

Server command for file server

java -cp .;jopt-simple-4.6.jar;json-20140107.jar;gson-2.6.2.jar HttpServerMain