import re

import osmgraph
from networkx.drawing.nx_agraph import write_dot


def nodes_tag_filter(node):
  if node.get('crossing') != 'traffic_signals':
    node.clear()


def ways_tag_filter(ways):
  for k, v in ways.iteritems():
    ways[k] = re.sub(r'[^\x00-\x7f]', r'', v)


def main():
  g = osmgraph.parse_file('map.osm', nodes_tag_filter=nodes_tag_filter, ways_tag_filter=ways_tag_filter)
  print 'Number of edges: {}'.format(len(g.edges()))
  print 'Number of nodes: {}'.format(len(g.nodes()))

  write_dot(g, 'map.dot')


if __name__ == '__main__':
  main()
