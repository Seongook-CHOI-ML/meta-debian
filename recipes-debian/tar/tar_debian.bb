#
# base recipe: meta/recipes-extended/tar/tar_1.30.bb
# base branch: master
# base commit: 63a4ff7cf5f7d1671ab85800bc2212dd9cd9748d
#

SUMMARY = "GNU file archiving program"
DESCRIPTION = "GNU tar saves many files together into a single tape \
or disk archive, and can restore individual files from the archive."
HOMEPAGE = "http://www.gnu.org/software/tar/"


inherit debian-package
require recipes-debian/sources/tar.inc

LICENSE = "GPLv3+"
LIC_FILES_CHKSUM = "file://COPYING;md5=d32239bcb673463ab874e80d47fae504"

FILESPATH_append = ":${COREBASE}/meta/recipes-extended/tar/tar"
SRC_URI += "file://musl_dirent.patch"

inherit autotools gettext texinfo

PACKAGECONFIG ??= ""
PACKAGECONFIG_append_class-target = " ${@bb.utils.filter('DISTRO_FEATURES', 'acl', d)}"

PACKAGECONFIG[acl] = "--with-posix-acls, --without-posix-acls, acl,"

EXTRA_OECONF += "DEFAULT_RMT_DIR=${base_sbindir}"

# Let aclocal use the relative path for the m4 file rather than the
# absolute since tar has a lot of m4 files, otherwise there might
# be an "Argument list too long" error when it is built in a long/deep
# directory.
acpaths = "-I ./m4"

do_install_append () {
	ln -s tar ${D}${bindir}/gtar
}

do_install_append_class-target() {
	if [ "${base_bindir}" != "${bindir}" ]; then
		install -d ${D}${base_bindir}
		mv ${D}${bindir}/tar ${D}${base_bindir}/tar
		mv ${D}${bindir}/gtar ${D}${base_bindir}/gtar
		rmdir ${D}${bindir}/
	fi
}

do_install_append_libc-musl() {
	rm -f ${D}${libdir}/charset.alias
	rmdir ${D}${libdir}
}

# add for ptest support
SRC_URI += " \
    file://run-ptest \
    file://0001-Fix-the-difflink-test.patch \
"

inherit ptest

do_compile_ptest() {
    oe_runmake -C ${B}/gnu/ check
    oe_runmake -C ${B}/lib/ check
    oe_runmake -C ${B}/rmt/ check
    oe_runmake -C ${B}/src/ check
    rm -rf ${S}/tests/testsuite
    oe_runmake -C ${B}/tests/ testsuite
    oe_runmake -C ${B}/tests/ genfile checkseekhole ckmtime
}

do_install_ptest() {
    install -d ${D}${PTEST_PATH}/tests/
    install --mode=755 ${B}/tests/atconfig ${D}${PTEST_PATH}/tests/
    sed -i "/abs_/d" ${D}${PTEST_PATH}/tests/atconfig
    echo "abs_builddir=${PTEST_PATH}/tests/" >> ${D}${PTEST_PATH}/tests/atconfig
    install --mode=755 ${B}/tests/atlocal ${D}${PTEST_PATH}/tests/
    sed -i "/PATH=/d" ${D}${PTEST_PATH}/tests/atlocal
    install --mode=755 ${B}/tests/genfile ${D}${PTEST_PATH}/tests/
    install --mode=755 ${B}/tests/checkseekhole ${D}${PTEST_PATH}/tests/
    install --mode=755 ${B}/tests/ckmtime ${D}${PTEST_PATH}/tests/
    install --mode=755 ${S}/tests/testsuite ${D}${PTEST_PATH}/tests/
    sed -i "s#@PTEST_PATH@#${PTEST_PATH}#g" ${D}${PTEST_PATH}/run-ptest
}

PACKAGES =+ "${PN}-rmt"

FILES_${PN}-rmt = "${base_sbindir}/rmt*"

inherit update-alternatives

ALTERNATIVE_PRIORITY = "100"

ALTERNATIVE_${PN} = "tar"
ALTERNATIVE_${PN}-rmt = "rmt"
ALTERNATIVE_${PN}_class-nativesdk = ""
ALTERNATIVE_${PN}-rmt_class-nativesdk = ""

ALTERNATIVE_LINK_NAME[tar] = "${base_bindir}/tar"
ALTERNATIVE_LINK_NAME[rmt] = "${base_sbindir}/rmt"

PROVIDES_append_class-native = " tar-replacement-native"
NATIVE_PACKAGE_PATH_SUFFIX = "/${PN}"

BBCLASSEXTEND = "native nativesdk"

CVE_PRODUCT = "gnu:tar"
RDEPENDS_${PN}-ptest += "bash"
