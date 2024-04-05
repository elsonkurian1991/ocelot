#include <stdlib.h>
#include <check.h>
START_TEST(ocelot_testcase1)
{
double __val0 = -79;
double __val1 = -96;



int __arg0 = __val0;
int __arg1 = __val1;
realMain(__arg0,__arg1);

/* REPLACE THE ASSERTION BELOW */
ck_assert_str_eq("OK", "OK");
}
END_TEST


START_TEST(ocelot_testcase3)
{
double __val0 = 77;
double __val1 = -67;



int __arg0 = __val0;
int __arg1 = __val1;
realMain(__arg0,__arg1);

/* REPLACE THE ASSERTION BELOW */
ck_assert_str_eq("OK", "OK");
}
END_TEST


START_TEST(ocelot_testcase4)
{
double __val0 = 42;
double __val1 = 72;



int __arg0 = __val0;
int __arg1 = __val1;
realMain(__arg0,__arg1);

/* REPLACE THE ASSERTION BELOW */
ck_assert_str_eq("OK", "OK");
}
END_TEST


START_TEST(ocelot_testcase2)
{
double __val0 = 10;
double __val1 = -69;



int __arg0 = __val0;
int __arg1 = __val1;
realMain(__arg0,__arg1);

/* REPLACE THE ASSERTION BELOW */
ck_assert_str_eq("OK", "OK");
}
END_TEST


Suite * ocelot_generated_eef2490a(void)
{
Suite *s;
TCase *temp_tc;

s = suite_create("ocelot_generated_eef2490a");

temp_tc = tcase_create("ocelot_testcase1");
tcase_add_test(temp_tc, ocelot_testcase1);
suite_add_tcase(s, temp_tc);

temp_tc = tcase_create("ocelot_testcase3");
tcase_add_test(temp_tc, ocelot_testcase3);
suite_add_tcase(s, temp_tc);

temp_tc = tcase_create("ocelot_testcase4");
tcase_add_test(temp_tc, ocelot_testcase4);
suite_add_tcase(s, temp_tc);

temp_tc = tcase_create("ocelot_testcase2");
tcase_add_test(temp_tc, ocelot_testcase2);
suite_add_tcase(s, temp_tc);

return s;
}

int main(void) {
int number_failed;
Suite *s;
SRunner *sr;

s = ocelot_generated_eef2490a();
sr = srunner_create(s);

srunner_run_all(sr, CK_NORMAL);
number_failed = srunner_ntests_failed(sr);
srunner_free(sr);
return (number_failed == 0) ? EXIT_SUCCESS : EXIT_FAILURE;
}