#include <stdlib.h>
#include <check.h>
START_TEST(ocelot_testcase4)
{
double __val0 = -65;
double __val1 = -95;
double __val2 = 148375404;
double __val3 = -394778469;



int __arg0 = __val0;
int __arg1 = __val1;
gTempVal1 = __val2;
gTempVal2 = __val3;
realMain(__arg0,__arg1);

/* REPLACE THE ASSERTION BELOW */
ck_assert_str_eq("OK", "OK");
}
END_TEST


START_TEST(ocelot_testcase3)
{
double __val0 = 8;
double __val1 = 64;
double __val2 = 616044358;
double __val3 = -1055001436;



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
double __val0 = -67;
double __val1 = 96;
double __val2 = 1445641199;
double __val3 = 1841937803;



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
double __val0 = 7;
double __val1 = -23;
double __val2 = 697117087;
double __val3 = -1214786972;



int __arg0 = __val0;
int __arg1 = __val1;
gTempVal1 = __val2;
gTempVal2 = __val3;
realMain(__arg0,__arg1);

/* REPLACE THE ASSERTION BELOW */
ck_assert_str_eq("OK", "OK");
}
END_TEST


START_TEST(ocelot_testcase5)
{
double __val0 = -99;
double __val1 = 21;
double __val2 = 1578335476;
double __val3 = 993654767;



int __arg0 = __val0;
int __arg1 = __val1;
gTempVal1 = __val2;
gTempVal2 = __val3;
realMain(__arg0,__arg1);

/* REPLACE THE ASSERTION BELOW */
ck_assert_str_eq("OK", "OK");
}
END_TEST


Suite * ocelot_generated_f7fc2aa2(void)
{
Suite *s;
TCase *temp_tc;

s = suite_create("ocelot_generated_f7fc2aa2");

temp_tc = tcase_create("ocelot_testcase4");
tcase_add_test(temp_tc, ocelot_testcase4);
suite_add_tcase(s, temp_tc);

temp_tc = tcase_create("ocelot_testcase3");
tcase_add_test(temp_tc, ocelot_testcase3);
suite_add_tcase(s, temp_tc);

temp_tc = tcase_create("ocelot_testcase2");
tcase_add_test(temp_tc, ocelot_testcase2);
suite_add_tcase(s, temp_tc);

temp_tc = tcase_create("ocelot_testcase1");
tcase_add_test(temp_tc, ocelot_testcase1);
suite_add_tcase(s, temp_tc);

temp_tc = tcase_create("ocelot_testcase5");
tcase_add_test(temp_tc, ocelot_testcase5);
suite_add_tcase(s, temp_tc);

return s;
}

int main(void) {
int number_failed;
Suite *s;
SRunner *sr;

s = ocelot_generated_f7fc2aa2();
sr = srunner_create(s);

srunner_run_all(sr, CK_NORMAL);
number_failed = srunner_ntests_failed(sr);
srunner_free(sr);
return (number_failed == 0) ? EXIT_SUCCESS : EXIT_FAILURE;
}