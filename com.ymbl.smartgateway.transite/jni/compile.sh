#!/bin/sh

EXSH=`pwd`/$0
EXDIR=$(dirname ${EXSH})

PRETARGET=/tmp/transite-target

cd ${EXDIR}

[ ! -d "iptables" ] && \
    git clone -b v1.4.21 git://git.netfilter.org/iptables && \
    rm -rf iptables/.git && \
    patch -p0 < patches/iptables-jni-1.4.21.patch && \
    cp -v com_ymbl_smartgateway_transite_TransiteActivator.c iptables/iptables/ && \
    cp -v com_ymbl_smartgateway_transite_TransiteActivator.h iptables/iptables/ && \
    cp -v dlog.h iptables/iptables/

isnew=`find com_ymbl_smartgateway_transite_TransiteActivator.c -newer iptables/iptables/com_ymbl_smartgateway_transite_TransiteActivator.c`
[ "$isnew" == "com_ymbl_smartgateway_transite_TransiteActivator.c" ] && \
    cp -v com_ymbl_smartgateway_transite_TransiteActivator.c iptables/iptables/

isnew=`find com_ymbl_smartgateway_transite_TransiteActivator.h -newer iptables/iptables/com_ymbl_smartgateway_transite_TransiteActivator.h`
[ "$isnew" == "com_ymbl_smartgateway_transite_TransiteActivator.h" ] && \
    cp -v com_ymbl_smartgateway_transite_TransiteActivator.h iptables/iptables/

isnew=`find dlog.h -newer iptables/iptables/dlog.h`
[ "$isnew" == "dlog.h" ] && \
    cp -v dlog.h iptables/iptables/

cd iptables
rm -rf ${PRETARGET}

[ ! -x "configure" ] && \
    ./autogen.sh && ./configure --prefix=${PRETARGET} --host=mips-en751221-linux-gnu
#   ./autogen.sh && ./configure --prefix=${PRETARGET}

make && make install
rm -rf ${EXDIR}/iptables/target
mv ${PRETARGET} ${EXDIR}/iptables/target
cp -v ${EXDIR}/iptables/target/lib/libtransite.so.0.1.0 ../../src/main/java/transite.so
cp -v ${EXDIR}/iptables/target/lib/libip4tc.so.0 ../../src/main/java/
cp -v ${EXDIR}/iptables/target/lib/libip6tc.so.0 ../../src/main/java/
cp -v ${EXDIR}/iptables/target/lib/libxtables.so.10 ../../src/main/java/
cp -av ${EXDIR}/iptables/target/lib/xtables ../../src/main/java/
