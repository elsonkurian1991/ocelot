#include <stdlib.h>
#include <check.h>
START_TEST(ocelot_testcase3)
{
double __val0 = 70;
double __val1 = -51;
double __val2 = -1880920189;
double __val3 = 570364009;



int __arg0 = __val0;
int __arg1 = __val1;
gTempVal1 = __val2;
gTempVal2 = __val3;
realMain(__arg0,__arg1);

/* REPLACE THE ASSERTION BELOW */
ck_assert_str_eq("OK", "OK");
}
END_TEST


START_TEST(ocelot_testcase1)
{
double __val0 = -87;
double __val1 = -54;
double __val2 = -45682367;
double __val3 = -2025515604;



int __arg0 = __val0;
int __arg1 = __val1;
gTempVal1 = __val2;
gTempVal2 = __val3;
realMain(__arg0,__arg1);

/* REPLACE THE ASSERTION BELOW */
ck_assert_str_eq("OK", "OK");
}
END_TEST


START_TEST(ocelot_testcase2)
{
double __val0 = 47;
double __val1 = 86;
double __val2 = 1567128299;
double __val3 = -1725863741;



int __arg0 = __val0;
int __arg1 = __val1;
gTempVal1 = __val2;
gTempVal2 = __val3;
realMain(__arg0,__arg1);

/* REPLACE THE ASSERTION BELOW */
ck_assert_str_eq("OK", "OK");
}
END_TEST


Suite * ocelot_generated_aa070d5c(void)
{
Suite *s;
TCase *temp_tc;

s = suite_create("ocelot_generated_aa070d5c");

temp_tc = tcase_create("ocelot_testcase3");
tcase_add_test(temp_tc, ocelot_testcase3);
suite_add_tcase(s, temp_tc);

temp_tc = tcase_create("ocelot_testcase1");
tcase_add_test(temp_tc, ocelot_testcase1);
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

s = ocelot_generated_aa070d5c();
sr = srunner_create(s);

srunner_run_all(sr, CK_NORMAL);
number_failed = srunner_ntests_failed(sr);
srunner_free(sr);
return (number_failed == 0) ? EXIT_SUCCESS : EXIT_FAILURE;
}