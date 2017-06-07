import datetime as dt
import fnmatch
import json
import os
from math import ceil

import matplotlib.dates as mdates
import numpy as np
from matplotlib import pyplot as plt

# stats_path = os.path.join('..', 'final')
# figs_path = os.path.join('..', 'final')
stats_path = os.path.join('..', 'stats')
figs_path = os.path.join('..', 'figures')
START_TIME = dt.datetime(2016, 1, 13, 0, 0, 0)
END_TIME = dt.datetime(2016, 1, 14, 0, 0, 0)


def cleanup(j):
  j['pickupWaitingTimes'] = map(lambda x: 1.0 * x / 60000, j['pickupWaitingTimes'])


def analyze_experiment(filename):
  with open(os.path.join(stats_path, filename), 'r') as f:
    j = json.load(f)
    timestamp = filename.split('_')[1].split('.')[0]
    print timestamp
    print j['args']
    # print j
    # print "\n".join(j.keys())
    cleanup(j)
    print 'pickupWaitingTimeAverage: ' + str(np.average(j['pickupWaitingTimes']))
    make_plots(timestamp, j)


def make_histogram(timestamp, j, title, k, xlabel):
  if len(j[k]) == 0: return
  plt.hist(j[k], 50, facecolor='green', alpha=0.75)
  plt.xlabel(xlabel)
  plt.ylabel('Count')
  plt.title(title)
  plt.grid(True)

  # plt.show()
  plt.savefig(os.path.join(figs_path, '{}_{}_{}.png'.format(k, j['args'], 'hist')), dpi=300)
  plt.close('all')


def make_boxplot(timestamp, j, title, k, xlabel):
  if len(j[k]) == 0: return
  plt.boxplot(j[k])
  plt.xlabel(" ".join(j['args']))
  plt.ylabel('Times [min]')
  axes = plt.gca()
  axes.set_ylim([0, 50])
  plt.title(title)

  # plt.show()
  plt.savefig(os.path.join(figs_path, '{}_{}_{}.png'.format(k, j['args'], 'boxplt')), dpi=300)
  plt.close('all')


def make_plots(timestamp, j):
  make_double_plot(timestamp, j, 'Amount of idle taxis and waiting customers', 'amountOfIdleTaxis', 'amountOfWaitingCustomers')
  # make_histogram(timestamp, j, 'Customer waiting time before pickup', 'pickupWaitingTimes', 'Waiting time (min)')
  make_histogram(timestamp, j, 'Route length reduction by trading', 'tradeProfits', 'Trade profit (km)')
  make_histogram(timestamp, j, 'Number of requests before pickup', 'numberOfRequests', 'Count')
  make_histogram(timestamp, j, 'Ratio of actual distance to shortest distance', 'travelTimeOverhead', 'Ratio')
  make_boxplot(timestamp, j, 'Pickup Waiting Times', 'pickupWaitingTimes', j['args'])


def average(arr, n):
  end = n * int(len(arr) / n)
  return np.mean(np.asarray(arr)[:end].reshape(-1, n), 1)


def to_time(tick, tick_diff):
  return START_TIME + dt.timedelta(seconds=(END_TIME - START_TIME).total_seconds() * tick / tick_diff)


def make_double_plot(timestamp, j, title, k1, k2, xlabel='Time'):
  fig, ax1 = plt.subplots()

  points = 500
  x_max = max(len(j[k1]), len(j[k2]))
  sample = int(ceil(1.0 * x_max / points))

  y1 = average(j[k1], sample)
  x1 = np.linspace(0, x_max, num=len(y1))
  x1_time = map(lambda x: to_time(x, x1[-1] - x1[0]), x1)
  dl = mdates.AutoDateLocator()
  plt.gca().xaxis.set_major_formatter(mdates.AutoDateFormatter(dl))
  plt.gca().xaxis.set_major_locator(dl)
  ax1.plot(x1_time, y1, 'r-')
  plt.gcf().autofmt_xdate()
  ax1.set_xlabel(xlabel)
  ax1.set_ylabel(k1, color='r')
  ax1.margins(0, 0.1)
  ax1.tick_params('y', colors='r')

  ax2 = ax1.twinx()
  y2 = average(j[k2], sample)
  x2 = np.linspace(0, x_max, num=len(y2))
  x2_time = map(lambda x: to_time(x, x2[-1] - x2[0]), x2)
  ax2.plot(x2_time, y2, 'b-')
  ax2.set_ylabel(k2, color='b')
  ax2.margins(0, 0.1)
  ax2.tick_params('y', colors='b')

  plt.title(title)
  # fig.tight_layout()
  # plt.show()
  plt.savefig(os.path.join(figs_path, '{}_{}_{}_{}.png'.format(timestamp, j['args'], k1, k2)), dpi=300)
  plt.close('all')


def combined_boxplots(jsons, title, k):
  data = map(lambda x: x[k], jsons)
  # xlabels = map(lambda x: " ".join(x['args']), jsons)
  xlabels = ['No improvements', 'Field', 'Field + trading']
  print xlabels
  plt.boxplot(data)
  plt.title(title)
  plt.xticks(range(1, len(xlabels) + 1), xlabels, rotation=45, ha='right')
  plt.tight_layout()
  plt.savefig(os.path.join(figs_path, '{}_combined_boxplots.png'.format(k)), dpi=300)
  plt.close('all')


def main():
  for f in os.listdir(stats_path):
    if fnmatch.fnmatch(f, 'stats_*.json'):
      analyze_experiment(f)


def create_combined_boxplots():
  jsons = []
  for f in os.listdir(stats_path):
    if fnmatch.fnmatch(f, 'stats_*.json'):
      with open(os.path.join(stats_path, f), 'r') as fl:
        j = json.load(fl)
        cleanup(j)
        jsons.append(j)
  combined_boxplots(jsons, 'Pickup waiting times', 'pickupWaitingTimes')
  combined_boxplots(jsons, 'Travel time overhead', 'travelTimeOverhead')


if __name__ == "__main__":
  main()
  create_combined_boxplots()
