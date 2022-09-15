# End-to-End Testing with Different Configurations

End-to-end testing of different Anchor Platform configurations can be accomplished with `docker-compose`. We will
maintain a base file (`integration-tests/docker-compose-configs/docker-compose.base.yaml`) and pass in an override
file (`integration-tests/docker-compose-configs/*/docker-compose.override.yaml`) to specify which config files to use
and what `end_to_end_tests` to run against the Anchor Platform configuration.


end_to_end_tests.py is a Python CLI tool used to test end-to-end **Anchor Platform + Anchor Server + Kafka + 
Horizon** data flows.

Please refer to [End to End Tests](/end-to-end-tests/README.md) for information on the end-to-end tests.


### Anchor Platform Configuration Files:
Each **directory** in `integration-tests/docker-compose-configs` contains all the 
configuration files necessary for a full Anchor Platform deployment. **Note**: Except for
`docker-compose-config.override.yaml`, all other files will replace the default ones if provided, and 
if not provided, the default ones from 
`integration-tests/docker-compose-configs/anchor-platform-default-configs` will be used.
1) `anchor-platform-config` - the configuration file for **Anchor Platform**
2) `anchor-reference-server-config.yaml` - the configuration file for the **Anchor Reference Server**
3) `assets-test.json` - the assets file to be used by the **Anchor Platform**
4) `docker-compose-config.override.yaml` - this file gets merged into `docker-compose-configs/docker-compose.base.yaml
when running docker-compose`. This file contains references to the specific files (the config directory) to be used
and which tests (end-to-end) are to be run against the setup.
5) `stellar.toml` - the TOML file thats served from Anchor Platform's `/.well-known/stellar.toml` endpoint

### Adding a New Configuration for Testing
1) Create a new directory in `integration-test/docker-compose-configs` 
2) Copy any configuration file(s) you're interested in modifying from 
`integration-tests/docker-compose-configs/anchor-platform-default-configs` into the new directory **(Note: you only 
need to copy the config files you're interested in modifying, `anchor-platform-default-configs/*` files will be 
used otherwise)**
3) Make whatever specific config changes you'd like to the copied file(s)
4) Update the `docker-compose-config.override.yaml` file to reference the configuration files in the newly created
directory.

   a) Replace `<new-config-directory>` with the name of the directory you created for this new configuration.

   b) Replace `<test-to-run1>` and/or `<test-to-run2>` with the name of the test(s) you'd like run against the newly added
   configuration (refer to: [End to End Tests](/end-to-end-tests/README.md)).

   ```text
   version: '2'
   services:
     anchor-platform-server:
       volumes:
         # add mounts for the new config directory
         - ./<new-config-directory>:/config_override
       
       anchor-reference-server:
         volumes:
           # add mounts for the new config directory
           - ./<new-config-directory>:/config_override
   
       end-to-end-tests:
         command: --domain host.docker.internal:8080 --tests <test-to-run1> --tests <test-to-run2>
   ```

5) At the top of the `docker-compose-config.override.yaml` file, document what this new configuration encompasses
6) Add this new configuration to the `Makefile` (Note: replace `<name-of-your-new-test>` and 
`<new-config-directory>`):
    ```text
    run-e2e-test-all:
    	make run-e2e-test-default-config
    	make run-e2e-test-allowlist
    	make run-e2e-test-unique-address
    	make <name-of-your-new-test>
    
    <name-of-your-new-test>:
		$(call run_tests,<name-of-your-new-test>)
    ```
1) Update the `end_to_end_tests.yml` GitHub Action by adding a new step to run the new configuration (Note: replace `<name-of-your-new-test>` and 
`<new-test-description>`):
    ```yaml
    - name: Run end-to-end tests (<new-test-description>)
      run: make <name-of-your-new-test>
    ```


### Running Tests:
1) Build the Anchor Platform and end-to-end test images:
   ```text
   make build-docker-compose-tests
   ```  
2) Create a `.env` file and add the **STELLAR_SECRET** to be used to run the end-to-end tests.
   
   a) **OMNIBUS_ALLOWLIST_KEYS** is used for the e2e allowlist test, one of the keys should be the public key that 
      corresponds to the <STELLAR_SECRET>. These comma separated values
      will be used for the `omnibusAllowList` value in `anchor-platform-allowlist/anchor-platform-config.yaml`
   ```text
   E2E_SECRET=<STELLAR_SECRET>
   OMNIBUS_ALLOWLIST_KEYS=<OMNIBUS_ALLOWLIST_KEYS>
   ```
3) Run the end-to-end test on all the different configs:
   ```text
   make run-e2e-test-all
   ```
or you can run individual tests:
   ```text
   make run-e2e-test-unique-address
   ```