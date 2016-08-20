# emr-resizer: Dynamically Resize EMR

This tool will allow for nodes to be dynamically added/removed to/from an EMR cluster.
It is intended to be used as part of processing workflow to increase and decrease the
number of nodes. The first step in the workflow would be to increase the number of nodes
and the last step would be to decrease the number of nodes.

## Usage

java -jar emr-resizer-1.0.0-bundle.jar  -cid=<id> -optype=<operation> -insttype=<type> -instcnt=<num> [-timeout=<num>] [-iam]

where:

  -cid=<id>
    The Amazon cluster identification string, such as 'j-34QCX7S1KA9DB'

  -optype=<operation>
    Where <operation> will be either 'increase' or 'decrease'.

  -insttype=<type>
    The Amazon instance type identification string. Currently, 'm4.2xlarge', 'm4.4xlarge'
    and 'm4.10xlarge' are supported.

  -instcnt=<num>
    Where <num> will be the number if instance to added or removed. 

  -timeout=<num>
    An optional parameter for the number of minutes to wait for the instance operation
    to complete before timing out, defaults to 20

  -iam
    An optional parameter to use the instance IAM role to authenticate
