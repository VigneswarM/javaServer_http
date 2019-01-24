HttpServer implementation
Normal download and get check
httpc get -v -h Accept:text/plain "http://127.0.0.1/post.json
httpc get -v -h Accept:text/plain "http://127.0.0.1/404.html

Security check
httpc post -v -h Accept:text/plain -f test.json "http://127.0.0.1/trick/get.json