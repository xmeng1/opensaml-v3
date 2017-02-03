import os
import shutil
import subprocess
from datetime import datetime, timedelta

#Adjust as desired
working_base = "/tmp/pkix-test-data"
openssl_config="openssl.cnf"
key_length=2048
signing_digest = "sha256"
cert_days = 365*10*3
ca_exts="ca_exts"
end_entity_exts="end_entity_exts"
altname_exts="altname_req"
crl_days = cert_days
crl_v2_exts="crl_ext"

now_utc = datetime.utcnow()

ca_config = {
    "root1-ca" :   {
        "subs" : {
            "inter1A-ca":  { 
                "subs": {
                    "inter1A1-ca": { },
                }
            },
            "inter1B-ca":  { },
        }
    },

    "root2-ca" : {
        "subs" : {
            "inter2A-ca":  { },
            "inter2B-ca":  { },
        }
    },

    "root3-ca" :   {
    },

}

ee_config = (
    # This one is the standard, normal, valid cert
    { "name": "foo-1A1-good", "san": "DNS:foo.example.org,URI:https://foo.example.org/sp", "cn" : "/CN=foo.example.org", "signer" : "inter1A1-ca", "days": cert_days},

    # This one is expired, only has validity duration of 10 seconds
    { "name": "foo-1A1-expired", "san": "DNS:foo.example.org,URI:https://foo.example.org/sp", "cn": "/CN=foo.example.org", "signer": "inter1A1-ca", 
        "days": None,  
        "startdate": now_utc,
        "enddate": now_utc + timedelta(seconds=10) },

    # This one will be revoked, and will appear in generated CRLs
    { "name": "foo-1A1-revoked", "san": "DNS:foo.example.org,URI:https://foo.example.org/sp", "cn": "/CN=foo.example.org", "signer": "inter1A1-ca", "days": cert_days},
)


# Start main()
def main():

    # Initialize all CAs
    for ca, config in ca_config.items():
        init_ca(ca, config, None)

    # Issue end entity certs from CAs
    for ee_data in ee_config:
        ee_name=ee_data['name']
        ee_keypath=os.path.join(os.getcwd(), ee_name+".key")
        ee_csrpath=os.path.join("/tmp", ee_name+".csr")
        ee_certpath=os.path.join(os.getcwd(), ee_name+".crt")

        duration_args = {}
        if "days" in ee_data and ee_data["days"]:
            duration_args["days"] = ee_data["days"]
        elif "startdate" in ee_data and "enddate" in ee_data:
            # the datetime instances should already have been specified in UTC, e.g. via datetime.utcnow()
            duration_args["days"] = None
            duration_args["startdate"] = ee_data["startdate"].strftime("%y%m%d%H%M%S") + "Z"
            duration_args["enddate"] = ee_data["enddate"].strftime("%y%m%d%H%M%S") + "Z"

        gen_key(ee_keypath)

        try:
            os.environ['SAN'] = ee_data['san']
            gen_csr(ee_keypath, ee_csrpath, ee_data['cn'], exts=altname_exts)
        finally:
            os.environ.pop('SAN')

        sign_csr(ee_csrpath, ee_certpath, build_ca_dirpath(ee_data['signer']), end_entity_exts, **duration_args)

    # Initial empty CRL
    gen_crl(build_ca_dirpath("inter1A1-ca"), os.path.join(os.getcwd(), "inter1A1-v1-empty.crl"))

    # Revoke "foo-1A1-revoked"
    revoke_cert(build_ca_dirpath("inter1A1-ca"), os.path.join(os.getcwd(), "foo-1A1-revoked.crt"))

    # Valid V1 CRL
    gen_crl(build_ca_dirpath("inter1A1-ca"), os.path.join(os.getcwd(), "inter1A1-v1.crl"))

    # Valid V2 CRL
    gen_crl(build_ca_dirpath("inter1A1-ca"), os.path.join(os.getcwd(), "inter1A1-v2.crl"), exts=crl_v2_exts)

    # Expired V1 CRL (well, will expire in 1 hour)
    gen_crl(build_ca_dirpath("inter1A1-ca"), os.path.join(os.getcwd(), "inter1A1-v1-expired.crl"), days=None, hours=1)


# End main()


##############################
### Utility functions here ###
##############################

def revoke_cert(cahome, cert_to_revoke):
    try:
        os.environ['CAHOME'] = cahome
        execute_openssl("ca -revoke {}", cert_to_revoke)
    finally:
        os.environ.pop('CAHOME')

def gen_crl(cahome, crlout, days=crl_days, hours=None, digest=signing_digest, exts=None):
    try:
        os.environ['CAHOME'] = cahome
        execute_openssl("ca -gencrl -out {} -md {} {} {} {}", crlout, digest,
            "-crldays " + str(days) if days else "", 
            "-crlhours " + str(hours) if hours else "",
            "-crlexts " + exts if exts else "")
    finally:
        os.environ.pop('CAHOME')

def sign_csr(csr, certout, cahome, exts, days=cert_days, startdate=None, enddate=None, digest=signing_digest):
    try:
        os.environ['CAHOME'] = cahome
        execute_openssl("ca -in {} -out {} -md {} -extensions {} {} {} {}", csr, certout, digest, exts, 
            "-days " + str(days) if days else "", 
            "-startdate " + startdate if startdate else "", 
            "-enddate " + enddate if enddate else "")
    finally:
        os.environ.pop('CAHOME')

def gen_csr(key, csrout, subject, exts=None, digest=signing_digest):
    execute_openssl("req -new -key {} -out {} -subj {}  -{} {}", key, csrout, subject, digest, "-reqexts " + exts if exts else "")

def gen_key(path, length=key_length):
    execute_openssl("genrsa -out {} {}", path, length)

def execute_openssl(cmd, *fmt_params):
    #print("execute_openssl:", cmd, fmt_params)
    to_execute = " ".join(["openssl", cmd, "-config", openssl_config]).format(*fmt_params)
    print("to_execute", to_execute)
    # Just splitting the string like this is quick/dirty/kludgy, doesn't work if the arg values have spaces, etc.
    subprocess.call(to_execute.split())

def init_new_ca_dir(rootdir):
    if os.path.exists(rootdir):
        shutil.rmtree(rootdir)
    os.makedirs(rootdir)
    os.mkdir(os.path.join(rootdir, "newcerts"))
    open(os.path.join(rootdir, "index.txt"), "x").close()
    with open(os.path.join(rootdir, "serial"), "x") as serial:
        print("01", file=serial)

def build_ca_dirpath(ca, base=working_base):
    return os.path.join(base, ca)

def init_ca(ca, config, parent):
    rootdir = build_ca_dirpath(ca)
    print("Initializing CA {} in root dir {}".format(ca, rootdir))
    init_new_ca_dir(rootdir)

    keypath = os.path.join(rootdir, "ca.key")
    csrpath = os.path.join(rootdir, "ca.csr")
    certpath = os.path.join(rootdir, "ca.crt")

    gen_key(keypath)

    if parent:
        # It's not a root CA, so generate a CSR and sign with the parent CA
        gen_csr(keypath, csrpath, "/CN="+ca)
        sign_csr(csrpath, certpath, build_ca_dirpath(parent), ca_exts) 
    else:
        # Create self-signed certs for all roots
        execute_openssl("req -new -x509 -key {} -out {} -days {} -subj {} -{} -extensions {}", keypath, certpath, cert_days, "/CN="+ca, signing_digest, ca_exts)

    # Copy the new CA key and cert out to the current working directory
    shutil.copy(keypath, os.path.join(os.getcwd(), ca+".key"))
    shutil.copy(certpath, os.path.join(os.getcwd(), ca+".crt"))

    if "subs" in config:
        for sub, subconfig in config["subs"].items():
            init_ca(sub, subconfig, ca)


if __name__ == "__main__":
    main()


