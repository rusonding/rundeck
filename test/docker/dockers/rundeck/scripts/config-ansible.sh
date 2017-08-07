#!/bin/bash
#/ does something ...
#/ usage: [..]

set -euo pipefail
IFS=$'\n\t'
readonly ARGS=("$@")
# <http://www.kfirlavi.com/blog/2012/11/14/defensive-bash-programming/>
# <http://redsymbol.net/articles/unofficial-bash-strict-mode/>

usage() {
      grep '^#/' <"$0" | cut -c4- # prints the #/ lines above as usage info
}
die(){
    echo >&2 "$@" ; exit 2
}

check_args(){
   : example to check args length
#  if [ ${#ARGS[@]} -lt 1 ] ; then
#        usage
#        exit 2
#    fi
}

# func(){
#    local FARGS=("$@")
#  #    echo $FUNCNAME $@
#  # set -x
#  # do something
#  # set -x
#}
setup_ansible(){
	cat >/etc/ansible/hosts <<END
[ansible-nodes]
$NODE1 ansible_host=$NODE1 ansible_port=22
test-ansible-node ansible_connection=local ansible_host=127.0.0.1
$RUNDECK_NODE ansible_connection=local
END


}

main() {
    check_args
    # use local vars
    #local i
    setup_ansible
}
main
