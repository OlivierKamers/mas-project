import osmgraph
from networkx.drawing.nx_agraph import write_dot


def nodes_tag_filter(node):
  if node.get('crossing') != 'traffic_signals':
    node.clear()


def ways_tag_filter(ways):
  pass


def main():
  g = osmgraph.parse_file('map.osm', nodes_tag_filter=nodes_tag_filter, ways_tag_filter=ways_tag_filter)
  print len(g.edges())
  print len(g.nodes())

  write_dot(g, 'map.dot')


if __name__ == '__main__':
  main()
