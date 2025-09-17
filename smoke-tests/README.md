# Create and vary a licence smoke tests

## Set up required

Install K6

```shell
brew install k6
```

To edit and run scripts in Intellij, install the K6 plugin.

I haven't been able to get the scripts to run in Intellij. If you'd like to try, then configure the plug in enter the
path to the k6 binary in Intellij settings under

```shell
Settings -> Tools -> K6 and set the k6 installation folder. 
```

For example on my laptop the folder is

```shell    
opt/homebrew/bin/k6
```

but as I said, I still couldn't get it to work.

To get intellisense in the k6 script files run

```shell
npm install
```

# Create secrets file

Create a file named cvl-smoke-tests.secrets in the smoke-tests directory. It should have the following contents.

```shell
user="a COM user name"
password="a COM password"
nomisId="a valid nomis id to use for creating licences etc"
```

# Run the tests

To run the tests from a terminal in the smoke-tests folder run

```shell
./run-smoke-tests.sh
```
